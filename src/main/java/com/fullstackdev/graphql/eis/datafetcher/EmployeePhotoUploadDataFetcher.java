package com.fullstackdev.graphql.eis.datafetcher;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.web.multipart.MultipartFile;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class EmployeePhotoUploadDataFetcher {
	
	@DgsMutation
	public String uploadEmployeePhoto(@InputArgument(name="emp_id") Integer empId, @InputArgument MultipartFile inputFile) throws IOException {
		
		 System.out.println("empId :"+empId);
		 
		Path uploadDir = Paths.get("uploaded-images");
		
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        String fileName = empId+"-"+inputFile.getOriginalFilename();
        
        Path newFile = uploadDir.resolve(fileName);
       
        try (OutputStream outputStream = Files.newOutputStream(newFile)) {
            outputStream.write(inputFile.getBytes());
        }
       
		return newFile.toUri().toString();
	}

}
