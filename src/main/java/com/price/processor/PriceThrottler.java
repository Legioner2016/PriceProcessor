package com.price.processor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * PriceThrottler class - observerable for PriceProcessors  
 * 
 * @author legioner
 *
 */
@Component("priceThrottler")
public class PriceThrottler implements PriceProcessor {
	
	private final int THREADS_COUNT = 4; 
	
	//Processors Map
	//concurrent add - remove processors
	private final Map<PriceProcessor, ExcecutorData> processors = new ConcurrentHashMap<>();

	//concurrent map - last rate of price
	private final Map<String, Double> currentState = new ConcurrentHashMap<>();
	
	//executorService
	ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT); 
	
	@PreDestroy
	public void destroy() {
		executorService.shutdown();
	}
	
	//Update all observers - call it's onPrice method 
	//Here i see such a problem:
	// 20:50 - new rate for EURRUB, but processing not finished (will be finished at 21:00)
	// 20:50 - 23:00 - don't receive any rates
	// 23:00 - new rate for EURUSD
	// so, rate for EURRUB will be updated with 2 hours lag
	// but, in task definition - "Some ccyPairs change rates 100 times a second" - so i ignore this problem  
	@Override
	public void onPrice(String ccyPair, double rate) {
		//Update rate maps
		currentState.put(ccyPair, rate);
		//Observer
		processors.forEach((k_, v_) -> {
			startThreadDoubleCheck(v_);
		});
	}

	//Add new processor
	@Override
	public void subscribe(PriceProcessor priceProcessor) {
		processors.put(priceProcessor, new ExcecutorData(new ProccesorExecutor(priceProcessor), null));
	}

	//Remove processor
	@Override
	public void unsubscribe(PriceProcessor priceProcessor) {
		processors.remove(priceProcessor);
	}
	
	//Synchronized thread start with double check
	//to avoid several starts on multithreads calls
	private void startThreadDoubleCheck(ExcecutorData executorData) {
		if (executorData.getResult() == null || executorData.getResult().isDone()) {
			synchronized (executorData) {
				if (executorData.getResult() == null || executorData.getResult().isDone()) {
					executorData.setResult(executorService.submit(executorData.getPe()));
				}	
			}
		}	
	}
	
	//Additional class - runable for start processing
	@RequiredArgsConstructor
	private class ProccesorExecutor implements Callable<Boolean> {
		private final PriceProcessor 	processor;
		
		@Override
		public Boolean call() throws Exception {
			currentState.forEach((k, v) -> {
				processor.onPrice(k, v);
			});
			return true;
		}
	}
	
	//Bundle executor with result (future)
	@Getter
	@AllArgsConstructor
	private class ExcecutorData {
		private final ProccesorExecutor pe;
		@Setter
		private Future<Boolean>	result;
	}

	
}
