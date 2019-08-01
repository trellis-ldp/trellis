package org.trellisldp.test;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.ResourceService;

class InMemoryResourceServiceTest implements ResourceServiceTests {
    
    private final InMemoryResourceService testService = new InMemoryResourceService();

    @Override
    public ResourceService getResourceService() {
        return testService;
    }
    
    @Test 
    void fuck(){}
}
