package org.numenta.nupic.network;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.MersenneTwister;

import rx.Subscriber;


public class RegionTest {
    
    /**
     *      L1
     *       |
     *      L2
     *       |
     *      L3
     *       |
     *      L4
     *          
     * Test that L1 receives the encoder from L4 (passed up during initialization).
     * Test that L1 receives the bucket mapping from L4 
     * Test that the Region's "head" points to L1, and "tail" points to L4
     * 
     * Test that adding two sensors causes an exception
     * 
     * NOTE: TAKE A LOOK AT AUTO-CONFIGURING KEY.INPUT_DIMENSIONS
     */
    @Ignore
    public void testMultiLayerAssemblyNoSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 50, 50 });
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
            .add(n.createLayer("2", p)
                .add(Anomaly.create(params)))
            .add(n.createLayer("3", p)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(new SpatialPooler())
                .add(MultiEncoder.builder().name("").build())) 
            .connect("1", "2")
            .connect("2", "3")
            .connect("3", "4");
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                
            }
        });
       
        //r1.compute(input);
    }
    
    /**
     *      L1
     *       |
     *      L2
     *       |
     *      L3
     *       |
     *      L4
     *          
     * Test that L1 receives the encoder from L4 (passed up during initialization).
     * Test that L1 receives the bucket mapping from L4 
     * Test that the Region's "head" points to L1, and "tail" points to L4
     * 
     * Test that adding two sensors causes an exception
     * 
     * NOTE: TAKE A LOOK AT AUTO-CONFIGURING KEY.INPUT_DIMENSIONS
     */
    @Test
    public void testMultiLayerAssemblyWithSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 50, 50 });
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
            .add(n.createLayer("2", p)
                .add(Anomaly.create(params)))
            .add(n.createLayer("3", p)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("days-of-week.csv"))))
                .add(new SpatialPooler()))
            .connect("1", "2")
            .connect("2", "3")
            .connect("3", "4");
        
        System.out.println("enc = " + n.getEncoder());
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            int idx = 0;
            
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                // Test Classifier and Anomaly output
                switch(idx) {
                    case 0: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertEquals(1.0, i.getClassification("dayOfWeek").getStats(1)[0], 0.0);
                            assertEquals(1, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 1: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertEquals(1.0, i.getClassification("dayOfWeek").getStats(1)[0], 0.0);
                            assertEquals(1, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 2: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 0.5, 0.5 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(2, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 3: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 
                                0.33333333333333333, 0.33333333333333333, 0.33333333333333333 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(3, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 4: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 0.25, 0.25, 0.25, 0.25 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(4, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 5: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 0.2, 0.2, 0.2, 0.2, 0.2 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(5, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 6: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 
                                0.16666666666666666, 0.16666666666666666,
                                0.16666666666666666, 0.16666666666666666, 
                                0.16666666666666666, 0.16666666666666666 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(6, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                }
                idx++;
            }
        });
       
        r1.start();
        
        try {
            r1.lookup("4").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
    }
    
    /**
     * For this test, add "repeat" functionality and prime the SP so that we get TM predictions
     */
    @Test
    public void test2LayerAssemblyWithSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("2/3", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("days-of-week.csv"))))
                .add(new SpatialPooler()))
            .connect("2/3", "4");
        
        final int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };
        
        // Observe the top layer
        r1.lookup("4").observe().subscribe(new Subscriber<Inference>() {
            int idx = 0;
            
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                assertTrue(Arrays.equals(inputs[idx++], i.getEncoding()));
            }
        });
        
        // Observe the bottom layer
        r1.lookup("2/3").observe().subscribe(new Subscriber<Inference>() {
            int idx = 0;
            
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                assertTrue(Arrays.equals(inputs[idx++], i.getEncoding()));
            }
        });
        
        // Observe the Region output
        r1.observe().subscribe(new Subscriber<Inference>() {
            int idx = 0;
            
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                assertTrue(Arrays.equals(inputs[idx++], i.getEncoding()));
            }
        });
        
        r1.start();
                    
        try {
            r1.lookup("4").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

}
