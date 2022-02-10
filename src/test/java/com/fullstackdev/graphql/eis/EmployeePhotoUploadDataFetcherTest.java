package com.fullstackdev.graphql.eis;

import com.fullstackdev.graphql.eis.datafetcher.EmployeePhotoUploadDataFetcher;
import com.fullstackdev.graphql.eis.scalar.DateScalar;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { DgsAutoConfiguration.class, DateScalar.class, EmployeePhotoUploadDataFetcher.class })
public class EmployeePhotoUploadDataFetcherTest {

	@Autowired
	DgsQueryExecutor dgsQueryExecutor;

	@Autowired
	EmployeePhotoUploadDataFetcher empPhotoUploader;

	@Test
	public void test_uploadEmployeePhoto() throws IOException {

		MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain",
				"Spring Framework".getBytes());
		String url = empPhotoUploader.uploadEmployeePhoto(1, multipartFile);
		
		assertThat(url).contains("test");
		
	}

}
