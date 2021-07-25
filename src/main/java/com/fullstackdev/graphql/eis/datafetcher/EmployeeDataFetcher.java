package com.fullstackdev.graphql.eis.datafetcher;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.fullstackdev.graphql.eis.entity.Department;
import com.fullstackdev.graphql.eis.entity.Employee;
import com.fullstackdev.graphql.eis.entity.Gender;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;




@DgsComponent
public class EmployeeDataFetcher {
	
	Department hrDepartment =  new Department(1,"HR");
	Department finDepartment =   new Department(2,"Finance");
	Department mktDepartment =   new Department(3,"Marketing");
	Department engDepartment =   new Department(4,"Engineering");
	
	private final List<Employee> emps = List.of(
            new Employee(1,"Sally", "Bateman", Gender.M,LocalDate.of(1980, 12, 11), LocalDate.of(2014, 12, 2) ,hrDepartment ),
            new Employee(2,"Jessie", "Fraser", Gender.M,LocalDate.of(1981, 11, 2), LocalDate.of(2016, 8, 12) , finDepartment),
            new Employee(3,"Marlon", "Frost", Gender.F,LocalDate.of(1978, 12, 9), LocalDate.of(2001, 1, 20) , engDepartment),
            new Employee(4,"Shantelle", "Fitzpatrick", Gender.M,LocalDate.of(1975, 12, 12), LocalDate.of(2011, 8, 11) , mktDepartment),
            new Employee(5,"Hermione", "Zhang", Gender.F,LocalDate.of(1981, 12, 20), LocalDate.of(2011, 12, 20) ,hrDepartment ),
            new Employee(6,"Sana", "Sinclair", Gender.M,LocalDate.of(1982, 5, 16), LocalDate.of(2008, 12, 20) ,finDepartment),
            new Employee(7,"Charlie", "Morrow", Gender.F,LocalDate.of(1980,8, 15), LocalDate.of(2007, 12, 20) , engDepartment),
            new Employee(8,"Samina", "Donovan", Gender.M,LocalDate.of(1980, 12, 21), LocalDate.of(2005, 12, 20) , engDepartment),
            new Employee(9,"Hughie", "Huang", Gender.F,LocalDate.of(1980, 12, 12), LocalDate.of(2005, 12, 20) , engDepartment),
            new Employee(10,"Firat", "Hanson", Gender.M,LocalDate.of(1975, 2, 2), LocalDate.of(2004, 12, 20) , engDepartment)
           
    );
	
	@PostConstruct
	public void setEmpstoDept() {
		
		List.of(hrDepartment,finDepartment,mktDepartment,engDepartment).forEach(dept -> {			
			 dept.setEmployees(emps.stream().filter(emp -> emp.getDepartment().getId().equals(dept.getId())).collect(Collectors.toList()));
		});
	}

    
    @DgsQuery
    public Optional<Employee> employee(@InputArgument Integer id) {
       
        return emps.stream().filter(e -> e.getId().equals(id)).findFirst();

    }
    
    @DgsQuery
    public List<Employee> employees() {
       
        return  emps;

    }
    
    @DgsQuery
    public List<Department> departments() {
    	
        return   emps.stream().map(e -> e.getDepartment()).collect(Collectors.toList());

    }
    
    @DgsQuery
    public List<Employee> getEmployeesByDeptId(@InputArgument Integer deptId) {       
        return  emps.stream().filter(emp -> emp.getDepartment().getId().equals(deptId)).collect(Collectors.toList());

    }

}
