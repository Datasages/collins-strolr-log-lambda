package com.collins.railwaynet.strolrloglambda.service;



import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.amazonaws.services.lambda.runtime.Context;

public class LogFileIndexerServiceTest {
	
	
	@Mock
	private Session hibernateSession;
	
	@Before
	public void setup() {
		System.out.println("Setup");
	}

	@Test
	public void testHandleRequest() {
//		To be implemented

	}

}
