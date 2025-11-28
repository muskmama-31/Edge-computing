package org.fog.healthsim;

import java.util.Calendar;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import org.cloudbus.cloudsim.distributions.ExponentialDistr;
import org.fog.entities.Sensor;
import org.fog.entities.FogBroker;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class SensorExample {

    public static void main(String[] args) {
        try {
            // Disable logs to keep output clean
            Log.disable();

            // Initialize CloudSim
            int numUsers = 1; // number of brokers
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUsers, calendar, traceFlag);

            // Create a broker (needed to get userId)
            FogBroker broker = new FogBroker("broker");
            int userId = broker.getId();

            String appId = "health_app";
            int gatewayDeviceId = 1;
            double latency = 2.0;

            // Temperature Sensor: deterministic every 5 ms
            Sensor tempSensor = new Sensor(
                    "TempSensor",
                    "TEMPERATURE",
                    userId,
                    appId,
                    new DeterministicDistribution(5));
            tempSensor.setGatewayDeviceId(gatewayDeviceId);
            tempSensor.setLatency(latency);

            // Heartbeat Sensor: deterministic every 2 ms
            Sensor heartSensor = new Sensor(
                    "HeartSensor",
                    "HEARTBEAT",
                    userId,
                    appId,
                    new DeterministicDistribution(2));
            heartSensor.setGatewayDeviceId(gatewayDeviceId);
            heartSensor.setLatency(latency);

            // Motion Sensor: exponential with mean 10 ms
            Distribution expDist = new Distribution() {
                ExponentialDistr exp = new ExponentialDistr(10);

                @Override
                public double getNextValue() {
                    return exp.sample();
                }

                @Override
                public int getDistributionType() {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public double getMeanInterTransmitTime() {
                    // TODO Auto-generated method stub
                    return 0;
                }
            };
            Sensor motionSensor = new Sensor(
                    "MotionSensor",
                    "MOTION",
                    userId,
                    appId,
                    expDist);
            motionSensor.setGatewayDeviceId(gatewayDeviceId);
            motionSensor.setLatency(latency);

            // Print confirmation
            System.out.println("Sensors created successfully:");
            System.out.println(tempSensor.getName() + " - Deterministic (5 ms)");
            System.out.println(heartSensor.getName() + " - Deterministic (2 ms)");
            System.out.println(motionSensor.getName() + " - Exponential (mean 10 ms)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
