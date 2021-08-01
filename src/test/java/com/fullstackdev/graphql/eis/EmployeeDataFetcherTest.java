package com.fullstackdev.graphql.eis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fullstackdev.graphql.eis.datafetcher.EmployeeDataFetcher;
import com.fullstackdev.graphql.eis.entity.Department;
import com.fullstackdev.graphql.eis.entity.Employee;
import com.fullstackdev.graphql.eis.entity.Gender;
import com.fullstackdev.graphql.eis.entity.SubmittedEmployee;
import com.fullstackdev.graphql.eis.scalar.DateScalar;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;

import graphql.ExecutionResult;

@SpringBootTest(classes = { DgsAutoConfiguration.class, DateScalar.class, EmployeeDataFetcher.class })
public class EmployeeDataFetcherTest {

	@Autowired
	DgsQueryExecutor dgsQueryExecutor;

	@MockBean
	EmployeeDataFetcher empDataFetcher;

	@BeforeEach
	public void before() {

		Department hrDepartment = new Department(1, "HR");
		Department finDepartment = new Department(2, "Finance");
		Department mktDepartment = new Department(3, "Marketing");
		Department engDepartment = new Department(4, "Engineering");

		Mockito.when(empDataFetcher.employees())
				.thenAnswer(invocation -> List.of(
						new Employee(1, "Sally", "Bateman", Gender.M, LocalDate.of(1980, 12, 11),
								LocalDate.of(2014, 12, 2), hrDepartment),
						new Employee(2, "Jessie", "Fraser", Gender.M, LocalDate.of(1981, 11, 2),
								LocalDate.of(2016, 8, 12), finDepartment),
						new Employee(3, "Marlon", "Frost", Gender.F, LocalDate.of(1978, 12, 9),
								LocalDate.of(2001, 1, 20), engDepartment)));

		Mockito.when(empDataFetcher.employee(1)).thenAnswer(invocation -> Optional.of(new Employee(1, "Sally",
				"Bateman", Gender.M, LocalDate.of(1980, 12, 11), LocalDate.of(2014, 12, 2), hrDepartment)));

		Mockito.when(empDataFetcher.createEmployee(any(SubmittedEmployee.class))).thenAnswer(invocation -> {

			return new Employee(11, invocation.getArgument(0, SubmittedEmployee.class).getFirst_name(),
					invocation.getArgument(0, SubmittedEmployee.class).getLast_name(),
					invocation.getArgument(0, SubmittedEmployee.class).getGender(),
					invocation.getArgument(0, SubmittedEmployee.class).getBirth_date(),
					invocation.getArgument(0, SubmittedEmployee.class).getHire_date(), hrDepartment);
		});

	}

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

		ExecutionResult createEmployeeResult = dgsQueryExecutor.execute(
				"mutation { createEmployee (employee : {first_name :\"Suresh\" , last_name : \"Gadupu\" , deptId : 1 ,gender : M , hire_date : \"2014-12-02\" , birth_date:\"1980-12-11\" }) {id first_name last_name gender hire_date birth_date } }");

		assertThat(createEmployeeResult.getErrors().isEmpty());
		verify(empDataFetcher).createEmployee(any(SubmittedEmployee.class));

	}

}
