package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import com.price.processor.PriceProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ComponentScan(basePackages = {"com.example.demo", "com.price.processor"})
class PriceProcessorApplicationTests {
	
	@Autowired
	@Qualifier("priceThrottler")
	PriceProcessor  priceThrottler;
	
	@Test
	void contextLoads() throws InterruptedException {
		var slowImpl = new PriceProcessorImpl(10000L, "EURRUB");
		var midImpl = new PriceProcessorImpl(3000L, "EURGBP");
		var fastImpl = new PriceProcessorImpl(10L, "EURUSD");
		var fastImpl2 = new PriceProcessorImpl(10L, "EURUSD");
		var fastImpl3 = new PriceProcessorImpl(10L, "EURUSD");
		
		priceThrottler.subscribe(slowImpl);
		priceThrottler.subscribe(midImpl);
		priceThrottler.subscribe(fastImpl);
		priceThrottler.subscribe(fastImpl2);
		priceThrottler.subscribe(fastImpl3);
		
		//priceThrottler.onPrice(null, 0);
		for (int i = 0; i < 5000; i++) {
			String pair = null;
			if (i % 10 == 0) {
				pair =  "EURRUB";
			} 
			else {
				switch (i % 2) {
					case 0: pair =  "EURUSD";
						break;
					case 1: pair =  "EURGBP";
						break;
				};
			}
			priceThrottler.onPrice(pair, (double)i);
			Thread.currentThread().sleep(7L);
		}
		
		priceThrottler.unsubscribe(fastImpl);
		
		for (int i = 5000; i < 10000; i++) {
			String pair = null;
			if (i % 10 == 0) {
				pair =  "EURRUB";
			} 
			else {
				switch (i % 2) {
					case 0: pair =  "EURUSD";
						break;
					case 1: pair =  "EURGBP";
						break;
				};
			}
			priceThrottler.onPrice(pair, (double)i);
			Thread.currentThread().sleep(7L);
		}

		
		Thread.currentThread().sleep(15000L);
		
		priceThrottler.onPrice("", 0d);
		
		Thread.currentThread().sleep(10000L);
		
		assertEquals(4998.0, fastImpl.getRates().get(fastImpl.getRates().size() - 1), 0.0000001d);
		assertEquals(9999.0, midImpl.getRates().get(midImpl.getRates().size() - 1), 0.0000001d);
		assertEquals(9990.0, slowImpl.getRates().get(slowImpl.getRates().size() - 1), 0.0000001d);
		
		
	}
	
	
	@RequiredArgsConstructor 
	private class PriceProcessorImpl implements PriceProcessor {
		private final Long pause;
		private final String pair;
		@Getter
		private final List<Double> rates = new LinkedList<>();

		@Override
		public void onPrice(String ccyPair, double rate) {
			if (pair.equals(ccyPair)) {
				try {
					Thread.currentThread().sleep(pause);
				} catch (InterruptedException e) {
					log.error("Sleep error", e);
				}
				rates.add(rate);
			}
		}

		@Override
		public void subscribe(PriceProcessor priceProcessor) {
			throw new IllegalCallerException();
		}

		@Override
		public void unsubscribe(PriceProcessor priceProcessor) {
			throw new IllegalCallerException();
		}
		
	} 

}
