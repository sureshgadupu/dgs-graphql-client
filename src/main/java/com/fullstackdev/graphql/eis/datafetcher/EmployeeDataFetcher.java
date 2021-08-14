package com.fullstackdev.graphql.eis.datafetcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

@DgsComponent
public class EmployeeDataFetcher {

	private FluxSink<Employee> employeeStream;
	private ConnectableFlux<Employee> employeePublisher;

	Department hrDepartment = new Department(1, "HR");
	Department finDepartment = new Department(2, "Finance");
	Department mktDepartment = new Department(3, "Marketing");
	Department engDepartment = new Department(4, "Engineering");

	private final List<Department> departmentList = new ArrayList<>(
			List.of(hrDepartment, finDepartment, mktDepartment, engDepartment));

	private final List<Employee> emps = new ArrayList<>(List.of(
			new Employee(1, "Sally", "Bateman", Gender.M, LocalDate.of(1980, 12, 11), LocalDate.of(2014, 12, 2),
					hrDepartment),
			new Employee(2, "Jessie", "Fraser", Gender.M, LocalDate.of(1981, 11, 2), LocalDate.of(2016, 8, 12),
					finDepartment),
			new Employee(3, "Marlon", "Frost", Gender.F, LocalDate.of(1978, 12, 9), LocalDate.of(2001, 1, 20),
					engDepartment),
			new Employee(4, "Shantelle", "Fitzpatrick", Gender.M, LocalDate.of(1975, 12, 12), LocalDate.of(2011, 8, 11),
					mktDepartment),
			new Employee(5, "Hermione", "Zhang", Gender.F, LocalDate.of(1981, 12, 20), LocalDate.of(2011, 12, 20),
					hrDepartment),
			new Employee(6, "Sana", "Sinclair", Gender.M, LocalDate.of(1982, 5, 16), LocalDate.of(2008, 12, 20),
					finDepartment),
			new Employee(7, "Charlie", "Morrow", Gender.F, LocalDate.of(1980, 8, 15), LocalDate.of(2007, 12, 20),
					engDepartment),
			new Employee(8, "Samina", "Donovan", Gender.M, LocalDate.of(1980, 12, 21), LocalDate.of(2005, 12, 20),
					engDepartment),
			new Employee(9, "Hughie", "Huang", Gender.F, LocalDate.of(1980, 12, 12), LocalDate.of(2005, 12, 20),
					engDepartment),
			new Employee(10, "Firat", "Hanson", Gender.M, LocalDate.of(1975, 2, 2), LocalDate.of(2004, 12, 20),
					engDepartment))

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

		return emps.stream().filter(e -> e.getId().equals(id)).findFirst();

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
				employee.getGender(), employee.getBirth_date(), employee.getHire_date());
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
		System.out.println(emp_id + " : " + dept_id);
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
