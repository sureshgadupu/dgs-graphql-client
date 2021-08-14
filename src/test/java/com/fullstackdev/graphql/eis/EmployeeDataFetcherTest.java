package com.fullstackdev.graphql.eis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fullstackdev.graphql.eis.datafetcher.EmployeeDataFetcher;
import com.fullstackdev.graphql.eis.entity.Employee;
import com.fullstackdev.graphql.eis.entity.Gender;
import com.fullstackdev.graphql.eis.scalar.DateScalar;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;

import graphql.ExecutionResult;

@SpringBootTest(classes = { DgsAutoConfiguration.class, DateScalar.class, EmployeeDataFetcher.class })
public class EmployeeDataFetcherTest {

	@Autowired
	DgsQueryExecutor dgsQueryExecutor;

	@Autowired
	EmployeeDataFetcher empDataFetcher;

	@Test
	public void test_employees() {

		List<String> firstNames = dgsQueryExecutor
				.executeAndExtractJsonPath("{ employees { id first_name last_name } }", "data.employees[*].first_name");
		assertThat(firstNames).contains("Sally", "Jessie", "Marlon");
	}

	@Test
	public void test_employee() {

		String empName = dgsQueryExecutor.executeAndExtractJsonPath("{ employee (id : 1) { id first_name last_name } }",
				"data.employee.last_name");
		assertThat(empName).isEqualTo("Bateman");
	}

	@Test
	public void test_createEmployee() throws JsonMappingException, JsonProcessingException {

		Employee emp = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
				"mutation { createEmployee (employee : {first_name :\"Suresh\" , last_name : \"Gadupu\" , deptId : 1 ,gender : M , hire_date : \"2014-12-02\" , birth_date:\"1980-12-11\" }) {id first_name last_name gender hire_date birth_date} }",
				"data.createEmployee", Employee.class);

		assertThat(emp).isNotNull();
		assertThat(emp.getFirst_name()).isEqualTo("Suresh");
		assertThat(emp.getLast_name()).isEqualTo("Gadupu");
		assertThat(emp.getGender()).isEqualTo(Gender.M);
		assertThat(emp.getHire_date()).isEqualTo(LocalDate.of(2014, 12, 02));

	}

	@Test
	public void test_notifyEmployeeCreation() throws JsonMappingException, JsonProcessingException {

		ExecutionResult notifyEmployeeCreationSubscription = dgsQueryExecutor.execute(
				"subscription  { notifyEmployeeCreation { id first_name last_name gender hire_date birth_date} }");

		Publisher<ExecutionResult> publisher = notifyEmployeeCreationSubscription.getData();

		publisher.subscribe(new Subscriber<ExecutionResult>() {
			@Override
			public void onSubscribe(Subscription s) {
				s.request(1);
				;
			}

			@Override
			public void onNext(ExecutionResult executionResult) {

				assertThat(executionResult.getErrors()).isEmpty();

				Map<String, Object> empData = executionResult.getData();

				try {

					ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
					Employee emp = objectMapper.convertValue(empData.get("notifyEmployeeCreation"), Employee.class);

					assertThat(emp.getFirst_name()).isEqualTo("Suresh");
					assertThat(emp.getLast_name()).isEqualTo("Gadupu");
					assertThat(emp.getGender()).isEqualTo(Gender.M);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			@Override
			public void onError(Throwable t) {
				System.out.println(t);
				assertThat(t).isNull();
			}

			@Override
			public void onComplete() {
			}
		});

		ExecutionResult createEmployeeResult = dgsQueryExecutor.execute(
				"mutation { createEmployee (employee : {first_name :\"Suresh\" , last_name : \"Gadupu\" , deptId : 1 ,gender : M , hire_date : \"2014-12-02\" , birth_date:\"1980-12-11\" }) {id first_name last_name gender hire_date birth_date } }");

	}

}
