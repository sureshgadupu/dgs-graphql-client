package com.fullstackdev.graphql.eis.entity;

public class SubmittedDepartment {

	private String dept_name;
	
	public SubmittedDepartment(String dept_name) {
		super();		
		this.dept_name = dept_name;
	}

	public String getDept_name() {
		return dept_name;
	}

	public void setDept_name(String dept_name) {
		this.dept_name = dept_name;
	}


}
