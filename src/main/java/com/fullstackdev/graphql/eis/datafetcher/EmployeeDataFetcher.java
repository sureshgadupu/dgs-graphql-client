package com.fullstackdev.graphql.eis.datafetcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.netflix.graphql.dgs.client.CustomGraphQLClient;
import com.netflix.graphql.dgs.client.CustomMonoGraphQLClient;
import com.netflix.graphql.dgs.client.DefaultGraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.HttpResponse;
import com.netflix.graphql.dgs.client.MonoGraphQLClient;
import com.netflix.graphql.dgs.client.RequestExecutor;
import com.netflix.graphql.dgs.client.WebClientGraphQLClient;
//import net.minidev.json.JSONObject;
import net.minidev.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

import com.fullstackdev.graphql.eis.entity.Department;
import com.fullstackdev.graphql.eis.entity.Employee;
import com.fullstackdev.graphql.eis.entity.Gender;
import com.fullstackdev.graphql.eis.entity.SubmittedDepartment;
import com.fullstackdev.graphql.eis.entity.SubmittedEmployee;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.DgsSubscription;
import com.netflix.graphql.dgs.InputArgument;

import graphql.GraphQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@DgsComponent
public class EmployeeDataFetcher {

	private FluxSink<Employee> employeeStream;
	private ConnectableFlux<Employee> employeePublisher;


	RestTemplate restTemplate = new RestTemplate();

	Department hrDepartment = new Department(1, "HR");
	Department finDepartment = new Department(2, "Finance");
	Department mktDepartment = new Department(3, "Marketing");
	Department engDepartment = new Department(4, "Engineering");

	private final List<Department> departmentList = new ArrayList<>(
			List.of(hrDepartment, finDepartment, mktDepartment, engDepartment));

	private final List<Employee> emps = new ArrayList<>(List.of(
			new Employee(1, "Sally", "Bateman", Gender.M, LocalDate.of(1980, 12, 11), LocalDate.of(2014, 12, 2),
					hrDepartment,"New York","US"),
			new Employee(2, "Jessie", "Fraser", Gender.M, LocalDate.of(1981, 11, 2), LocalDate.of(2016, 8, 12),
					finDepartment, "New Jersey","US"),
			new Employee(3, "Marlon", "Frost", Gender.F, LocalDate.of(1978, 12, 9), LocalDate.of(2001, 1, 20),
					engDepartment, "Auckland","NZ"),
			new Employee(4, "Shantelle", "Fitzpatrick", Gender.M, LocalDate.of(1975, 12, 12), LocalDate.of(2011, 8, 11),
					mktDepartment,"Wellington","NZ"),
			new Employee(5, "Hermione", "Zhang", Gender.F, LocalDate.of(1981, 12, 20), LocalDate.of(2011, 12, 20),
					hrDepartment,"Wellington","NZ"),
			new Employee(6, "Sana", "Sinclair", Gender.M, LocalDate.of(1982, 5, 16), LocalDate.of(2008, 12, 20),
					finDepartment,"Delhi" , "IN"),
			new Employee(7, "Charlie", "Morrow", Gender.F, LocalDate.of(1980, 8, 15), LocalDate.of(2007, 12, 20),
					engDepartment,"Delhi" , "IN"),
			new Employee(8, "Samina", "Donovan", Gender.M, LocalDate.of(1980, 12, 21), LocalDate.of(2005, 12, 20),
					engDepartment,"Delhi" , "IN"),
			new Employee(9, "Hughie", "Huang", Gender.F, LocalDate.of(1980, 12, 12), LocalDate.of(2005, 12, 20),
					engDepartment, "Hyderabad" , "IN"),
			new Employee(10, "Firat", "Hanson", Gender.M, LocalDate.of(1975, 2, 2), LocalDate.of(2004, 12, 20),
					engDepartment, "Hyderabad" , "IN"))

	);

	@PostConstruct
	public void init() {

		departmentList.forEach(dept -> {
			dept.setEmployees(emps.stream().filter(emp -> emp.getDepartment().getId().equals(dept.getId()))
					.collect(Collectors.toList()));
		});

		Flux<Employee> publisher = Flux.create(emitter -> {
			employeeStream = emitter;
		});

		employeePublisher = publisher.publish();
		employeePublisher.connect();
	}

	@DgsQuery
	public Optional<Employee> employee(@InputArgument Integer id) {

		Optional<Employee> employee = emps.stream().filter(e -> e.getId().equals(id)).findFirst();

		if( employee.isPresent()) {
			setEmpCityTemp(employee.get());
			//setEmpCityTemp2(employee.get());
			//setEmpCityTemp3(employee.get());
			//setEmpCityTemp4(employee.get());

		}

		return employee;
	}

	private void setEmpCityTemp(Employee emp) {

		WebClient webClient = WebClient.create("https://graphql-weather-api.herokuapp.com/");

		WebClientGraphQLClient client = MonoGraphQLClient.createWithWebClient(webClient);

		Map<String, Object> variables = new HashMap<>();


		Map<String, Object> config = new HashMap<>();
		config.put("units","metric");

		variables.put("name" , emp.getCity()) ;
		variables.put("country" , emp.getCountry()) ;
		variables.put("config", config) ;

		String query  = 	 "query GetCityTemp ($name : String!, $country : String, $config: ConfigInput ){"+
									"getCityByName(name :$name, country : $country, config: $config){"+
										"name "+
										"weather { "+
										"temperature { "+
											"actual "+
										"} "+
									"}"+
								"} "+
							"}";

		try {
			Mono<GraphQLResponse> graphQLResponseMono =  client.reactiveExecuteQuery(query,variables);

			Mono<Double> temperature = graphQLResponseMono.map(r ->   r.extractValue("getCityByName.weather.temperature.actual"));
			temperature.subscribe();
			emp.setTemperature(temperature.block());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	// This method demonstrates converting graphql json response
	private void setEmpCityTemp2(Employee emp) {

		WebClient webClient = WebClient.create("https://graphql-weather-api.herokuapp.com/");

		WebClientGraphQLClient client = MonoGraphQLClient.createWithWebClient(webClient,headers ->{  headers.add("myheader", "test");
			headers.add("custom header", "custom value");});

		Map<String, Object> variables = new HashMap<>();


		Map<String, Object> config = new HashMap<>();
		config.put("units","metric");

		variables.put("name" , emp.getCity()) ;
		variables.put("country" , emp.getCountry()) ;
		variables.put("config", config) ;

		String query  = 	 "query GetCityTemp ($name : String!, $country : String, $config: ConfigInput ){"+
				"getCityByName(name :$name, country : $country, config: $config){"+
				"name "+
				"weather { "+
				"temperature { "+
				"actual "+
				"} "+
				"}"+
				"} "+
				"}";

		try {
			Mono<GraphQLResponse> graphQLResponseMono =  client.reactiveExecuteQuery(query,variables);

			Mono<LinkedHashMap> obj = graphQLResponseMono.flatMap(r ->  {

				if(r.extractValue("getCityByName") == null) {
					return Mono.empty();
					//return new LinkedHashMap();
				} else {
					return Mono.just(r.extractValue("getCityByName"));

				}
			} );



			obj.subscribe();


			ObjectMapper mapper = new ObjectMapper();
			String jsonResult = "";
			if(obj.blockOptional().isPresent()) {

				 jsonResult = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(obj.block());

				System.out.println(jsonResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Demonstrates usage of custom HTTP client (GraphQLClient: For blocking HTTP clients)
	private void setEmpCityTemp3(Employee emp) {

		Map<String, Object> variables = new HashMap<>();


		Map<String, Object> config = new HashMap<>();
		config.put("units","metric");

		variables.put("name" , emp.getCity()) ;
		variables.put("country" , emp.getCountry()) ;
		variables.put("config", config) ;

		String query  = 	 "query GetCityTemp ($name : String!, $country : String, $config: ConfigInput ){"+
				"getCityByName(name :$name, country : $country, config: $config){"+
				"name "+
				"weather { "+
				"temperature { "+
				"actual "+
				"} "+
				"}"+
				"} "+
				"}";

		String graphql_endpoint = "https://graphql-weather-api.herokuapp.com/";

		try {
			CustomGraphQLClient client = GraphQLClient.createCustom(graphql_endpoint,  (url, headers, body) -> {
				HttpHeaders httpHeaders = new HttpHeaders();
				headers.forEach(httpHeaders::addAll);
				ResponseEntity<String> exchange = restTemplate.exchange(graphql_endpoint, HttpMethod.POST, new HttpEntity<>(body, httpHeaders),String.class);
				return new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody());
			});

			GraphQLResponse graphQLResponse = client.executeQuery(query, variables, "GetCityTemp");
			Double temp = graphQLResponse.extractValueAsObject("getCityByName.weather.temperature.actual", Double.class);

			System.out.println(temp);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Demonstrates usage of custom HTTP client (MonoGraphQLClient: For non-blocking HTTP clients.)

	private void setEmpCityTemp4(Employee emp) {

		Map<String, Object> variables = new HashMap<>();


		Map<String, Object> config = new HashMap<>();
		config.put("units","metric");

		variables.put("name" , emp.getCity()) ;
		variables.put("country" , emp.getCountry()) ;
		variables.put("config", config) ;

		String query  = 	 "query GetCityTemp ($name : String!, $country : String, $config: ConfigInput ){"+
				"getCityByName(name :$name, country : $country, config: $config){"+
				"name "+
				"weather { "+
				"temperature { "+
				"actual "+
				"} "+
				"}"+
				"} "+
				"}";

		String graphql_endpoint = "https://graphql-weather-api.herokuapp.com/";

		try {
			CustomMonoGraphQLClient client = MonoGraphQLClient.createCustomReactive(graphql_endpoint, (requestUrl, headers, body) -> {
				HttpHeaders httpHeaders = new HttpHeaders();
				headers.forEach(httpHeaders::addAll);
				ResponseEntity<String> exchange = restTemplate.exchange(graphql_endpoint, HttpMethod.POST, new HttpEntity<>(body, httpHeaders),String.class);
				return Mono.just(new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody(), exchange.getHeaders()));
			});


			Mono<GraphQLResponse> graphQLResponseMono  = client.reactiveExecuteQuery(query, variables);
			Mono<Double> temperature = graphQLResponseMono.map(r -> r.extractValueAsObject("getCityByName.weather.temperature.actual",Double.class));

			temperature.subscribe();
			//emp.setTemperature(temperature.block());

			System.out.println(temperature.block());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@DgsQuery
	public List<Employee> employees() {

		return emps;

	}

	@DgsQuery
	public Set<Department> departments() {

		return emps.stream().map(e -> e.getDepartment()).collect(Collectors.toSet());

	}

	@DgsQuery
	public List<Employee> getEmployeesByDeptId(@InputArgument Integer deptId) {
		return emps.stream().filter(emp -> emp.getDepartment().getId().equals(deptId)).collect(Collectors.toList());

	}

	@DgsMutation
	public Employee createEmployee(@InputArgument SubmittedEmployee employee) {

		Employee emp = new Employee(emps.size() + 1, employee.getFirst_name(), employee.getLast_name(),
				employee.getGender(), employee.getBirth_date(), employee.getHire_date(),employee.getCity(),employee.getCountry());
		Optional<Department> optionalDept = departmentList.stream().filter(dept -> dept.getId() == employee.getDeptId())
				.findFirst();
		if (optionalDept.isPresent()) {
			emp.setDepartment(optionalDept.get());
		} else {
			throw new GraphQLException("Department does not exists with id " + employee.getDeptId());
		}
		emps.add(emp);

		employeeStream.next(emp);
		return emp;

	}

	@DgsMutation
	public Department createDepartment(@InputArgument SubmittedDepartment submittedDepartment) {

		Department department = new Department(departmentList.size() + 1, submittedDepartment.getDept_name());
		departmentList.add(department);
		return department;

	}

	@DgsMutation
	public Boolean updateEmpDepartment(@InputArgument Integer emp_id, @InputArgument Integer dept_id) {

		Optional<Department> optionalDept = departmentList.stream().filter(dept -> dept.getId() == dept_id).findFirst();
		Optional<Employee> optionalEmp = emps.stream().filter(emp -> emp.getId().equals(emp_id)).findFirst();
		if (optionalDept.isPresent() && optionalEmp.isPresent()) {
			optionalEmp.get().setDepartment(optionalDept.get());
			return true;
		}
		return false;

	}

	@DgsSubscription
	public Publisher<Employee> notifyEmployeeCreation() {
		
		return employeePublisher;
	}

}
