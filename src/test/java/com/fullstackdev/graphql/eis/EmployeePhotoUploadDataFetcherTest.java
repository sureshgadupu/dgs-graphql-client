package com.fullstackdev.graphql.eis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.fullstackdev.graphql.eis.datafetcher.EmployeePhotoUploadDataFetcher;
import com.fullstackdev.graphql.eis.entity.Image;
import com.fullstackdev.graphql.eis.scalar.DateScalar;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;

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
