package org.fog.healthsim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class HeterogeneousFogExample {

    private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    private static List<Sensor> sensors = new ArrayList<Sensor>();
    private static List<Actuator> actuators = new ArrayList<Actuator>();
    private static int numOfAreas = 2;
    private static int numOfDevicesPerArea = 2;

    public static void main(String[] args) {

        Log.printLine("Starting Heterogeneous Fog Nodes Simulation...");

        try {
            Log.disable();
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            String appId1 = "iot_monitoring";
            String appId2 = "video_streaming";
            String appId3 = "smart_city";

            FogBroker broker = new FogBroker("broker");

            Application iotApp = createIoTMonitoringApplication(appId1, broker.getId());
            Application videoApp = createVideoStreamingApplication(appId2, broker.getId());
            Application smartCityApp = createSmartCityApplication(appId3, broker.getId());

            createHeterogeneousFogDevices(broker.getId());
            createSensorsAndActuators(broker.getId(), appId1, appId2, appId3);

            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

            configureModulePlacement(controller, iotApp, videoApp, smartCityApp);

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            Log.printLine("Heterogeneous Fog Nodes Simulation finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void createHeterogeneousFogDevices(int userId) {

        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
        cloud.setParentId(-1);
        fogDevices.add(cloud);

        for (int i = 0; i < numOfAreas; i++) {
            FogDevice edgeDataCenter = createFogDevice("edge-dc-" + i, 2800, 4000, 10000, 10000, 1, 0.0, 107.339,
                    83.4333);
            edgeDataCenter.setParentId(cloud.getId());
            edgeDataCenter.setUplinkLatency(100);
            fogDevices.add(edgeDataCenter);

            FogDevice fogGateway = createFogDevice("fog-gateway-" + i, 2800, 4000, 10000, 270, 2, 0.0, 107.339,
                    83.4333);
            fogGateway.setParentId(edgeDataCenter.getId());
            fogGateway.setUplinkLatency(4);
            fogDevices.add(fogGateway);

            for (int j = 0; j < numOfDevicesPerArea; j++) {
                FogDevice fogNode = createFogDevice("fog-node-" + i + "-" + j, 1000, 1000, 10000, 270, 3, 0.0, 87.53,
                        82.44);
                fogNode.setParentId(fogGateway.getId());
                fogNode.setUplinkLatency(2);
                fogDevices.add(fogNode);

                FogDevice iotDevice = createFogDevice("iot-device-" + i + "-" + j, 500, 1000, 10000, 270, 4, 0.0, 87.53,
                        82.44);
                iotDevice.setParentId(fogNode.getId());
                iotDevice.setUplinkLatency(1);
                fogDevices.add(iotDevice);
            }
        }

        Log.printLine("Created " + fogDevices.size() + " heterogeneous fog devices");
    }

    private static void createSensorsAndActuators(int userId, String appId1, String appId2, String appId3) {

        int deviceIndex = 0;
        for (FogDevice device : fogDevices) {
            if (device.getName().startsWith("iot-device")) {

                Sensor tempSensor = new Sensor("temp-sensor-" + deviceIndex, "TEMP", userId, appId1,
                        new DeterministicDistribution(5000));
                sensors.add(tempSensor);
                tempSensor.setGatewayDeviceId(device.getId());
                tempSensor.setLatency(1.0);

                Actuator hvacActuator = new Actuator("hvac-actuator-" + deviceIndex, userId, appId1, "HVAC_CONTROL");
                actuators.add(hvacActuator);
                hvacActuator.setGatewayDeviceId(device.getId());
                hvacActuator.setLatency(1.0);

                Sensor cameraSensor = new Sensor("camera-sensor-" + deviceIndex, "CAMERA", userId, appId2,
                        new DeterministicDistribution(40));
                sensors.add(cameraSensor);
                cameraSensor.setGatewayDeviceId(device.getId());
                cameraSensor.setLatency(1.0);

                Actuator displayActuator = new Actuator("display-actuator-" + deviceIndex, userId, appId2, "DISPLAY");
                actuators.add(displayActuator);
                displayActuator.setGatewayDeviceId(device.getId());
                displayActuator.setLatency(1.0);

                Sensor trafficSensor = new Sensor("traffic-sensor-" + deviceIndex, "TRAFFIC_DATA", userId, appId3,
                        new DeterministicDistribution(2000));
                sensors.add(trafficSensor);
                trafficSensor.setGatewayDeviceId(device.getId());
                trafficSensor.setLatency(1.0);

                Actuator trafficLightActuator = new Actuator("traffic-light-" + deviceIndex, userId, appId3,
                        "TRAFFIC_CONTROL");
                actuators.add(trafficLightActuator);
                trafficLightActuator.setGatewayDeviceId(device.getId());
                trafficLightActuator.setLatency(1.0);

                deviceIndex++;
            }
        }

        Log.printLine("Created " + sensors.size() + " sensors and " + actuators.size() + " actuators");
    }

    private static Application createIoTMonitoringApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("temp_processor", 10);
        application.addAppModule("data_analyzer", 10);
        application.addAppModule("alert_manager", 10);

        application.addAppEdge("TEMP", "temp_processor", 1000, 500, "TEMP_DATA", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("temp_processor", "data_analyzer", 2000, 2000, "PROCESSED_TEMP", Tuple.UP,
                AppEdge.MODULE);
        application.addAppEdge("data_analyzer", "alert_manager", 100, 1000, "ANALYSIS_RESULT", Tuple.UP,
                AppEdge.MODULE);
        application.addAppEdge("alert_manager", "HVAC_CONTROL", 100, 28, "HVAC_COMMANDS", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("temp_processor", "TEMP", "PROCESSED_TEMP", new FractionalSelectivity(1.0));
        application.addTupleMapping("data_analyzer", "PROCESSED_TEMP", "ANALYSIS_RESULT",
                new FractionalSelectivity(0.05));
        application.addTupleMapping("alert_manager", "ANALYSIS_RESULT", "HVAC_COMMANDS",
                new FractionalSelectivity(0.1));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("TEMP");
                add("temp_processor");
                add("data_analyzer");
                add("alert_manager");
                add("HVAC_CONTROL");
            }
        });

        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };

        application.setLoops(loops);

        return application;
    }

    private static Application createVideoStreamingApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("motion_detector", 10);
        application.addAppModule("object_detector", 10);
        application.addAppModule("video_encoder", 10);

        application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "RAW_VIDEO", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("motion_detector", "object_detector", 2000, 1000, "MOTION_VIDEO_STREAM", Tuple.UP,
                AppEdge.MODULE);
        application.addAppEdge("object_detector", "video_encoder", 1000, 1000, "DETECTED_OBJECTS", Tuple.UP,
                AppEdge.MODULE);
        application.addAppEdge("video_encoder", "DISPLAY", 1000, 1000, "PROCESSED_VIDEO", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("motion_detector", "RAW_VIDEO", "MOTION_VIDEO_STREAM",
                new FractionalSelectivity(0.9));
        application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECTS",
                new FractionalSelectivity(1.0));
        application.addTupleMapping("video_encoder", "DETECTED_OBJECTS", "PROCESSED_VIDEO",
                new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("CAMERA");
                add("motion_detector");
                add("object_detector");
                add("video_encoder");
                add("DISPLAY");
            }
        });

        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };

        application.setLoops(loops);

        return application;
    }

    private static Application createSmartCityApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("traffic_analyzer", 10);
        application.addAppModule("city_coordinator", 10);
        application.addAppModule("decision_engine", 10);

        application.addAppEdge("TRAFFIC_DATA", "traffic_analyzer", 2000, 2000, "TRAFFIC_INFO", Tuple.UP,
                AppEdge.SENSOR);
        application.addAppEdge("traffic_analyzer", "city_coordinator", 1000, 1000, "TRAFFIC_ANALYSIS", Tuple.UP,
                AppEdge.MODULE);
        application.addAppEdge("city_coordinator", "decision_engine", 500, 1000, "CITY_STATUS", Tuple.UP,
                AppEdge.MODULE);
        application.addAppEdge("decision_engine", "TRAFFIC_CONTROL", 100, 28, "CONTROL_COMMANDS", Tuple.DOWN,
                AppEdge.ACTUATOR);

        application.addTupleMapping("traffic_analyzer", "TRAFFIC_INFO", "TRAFFIC_ANALYSIS",
                new FractionalSelectivity(1.0));
        application.addTupleMapping("city_coordinator", "TRAFFIC_ANALYSIS", "CITY_STATUS",
                new FractionalSelectivity(0.1));
        application.addTupleMapping("decision_engine", "CITY_STATUS", "CONTROL_COMMANDS",
                new FractionalSelectivity(0.05));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("TRAFFIC_DATA");
                add("traffic_analyzer");
                add("city_coordinator");
                add("decision_engine");
                add("TRAFFIC_CONTROL");
            }
        });

        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };

        application.setLoops(loops);

        return application;
    }

    private static void configureModulePlacement(Controller controller, Application iotApp, Application videoApp,
            Application smartCityApp) {

        ModuleMapping moduleMapping_iot = ModuleMapping.createModuleMapping();
        ModuleMapping moduleMapping_video = ModuleMapping.createModuleMapping();
        ModuleMapping moduleMapping_smart = ModuleMapping.createModuleMapping();

        for (FogDevice device : fogDevices) {
            if (device.getName().startsWith("cloud")) {
                moduleMapping_iot.addModuleToDevice("alert_manager", device.getName());
            }
            if (device.getName().startsWith("edge-dc")) {
                moduleMapping_iot.addModuleToDevice("data_analyzer", device.getName());
            }
            if (device.getName().startsWith("fog-node")) {
                moduleMapping_iot.addModuleToDevice("temp_processor", device.getName());
            }
        }

        for (FogDevice device : fogDevices) {
            if (device.getName().startsWith("edge-dc")) {
                moduleMapping_video.addModuleToDevice("video_encoder", device.getName());
            }
            if (device.getName().startsWith("fog-gateway")) {
                moduleMapping_video.addModuleToDevice("object_detector", device.getName());
            }
            if (device.getName().startsWith("fog-node")) {
                moduleMapping_video.addModuleToDevice("motion_detector", device.getName());
            }
        }

        for (FogDevice device : fogDevices) {
            if (device.getName().startsWith("cloud")) {
                moduleMapping_smart.addModuleToDevice("decision_engine", device.getName());
            }
            if (device.getName().startsWith("edge-dc")) {
                moduleMapping_smart.addModuleToDevice("city_coordinator", device.getName());
            }
            if (device.getName().startsWith("fog-node")) {
                moduleMapping_smart.addModuleToDevice("traffic_analyzer", device.getName());
            }
        }

        controller.submitApplication(iotApp, 0,
                new ModulePlacementMapping(fogDevices, iotApp, moduleMapping_iot));
        controller.submitApplication(videoApp, 0,
                new ModulePlacementMapping(fogDevices, videoApp, moduleMapping_video));
        controller.submitApplication(smartCityApp, 0,
                new ModulePlacementMapping(fogDevices, smartCityApp, moduleMapping_smart));

        Log.printLine("Module placement configured for all applications");
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level,
            double ratePerMips, double busyPower, double idlePower) {

        List<Pe> peList = new ArrayList<Pe>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHostUtilizationHistory host = new PowerHostUtilizationHistory(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower));

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }
}