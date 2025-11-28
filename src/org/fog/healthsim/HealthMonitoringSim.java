package org.fog.healthsim;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.cloudbus.cloudsim.Pe;

import java.util.*;

public class HealthMonitoringSim {

    private static List<FogDevice> fogDevices = new ArrayList<>();
    private static List<Sensor> sensors = new ArrayList<>();
    private static List<Actuator> actuators = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Log.printLine("=== Starting Smart Home Temperature Monitoring System ===");

        Log.disable();
        CloudSim.init(1, Calendar.getInstance(), false);

        FogBroker broker = new FogBroker("smart-home-broker");
        String appId = "temperature_monitoring_app";

        FogDevice cloud = createFogDevice("cloud", 44800, 40000,
                100000, 100000, 0, 0.01, 107.339, 83.4333);
        cloud.setParentId(-1);
        fogDevices.add(cloud);
        Log.printLine("Cloud Node created: " + cloud.getName());

        FogDevice homeGateway = createFogDevice("Home-Gateway", 8000, 16384,
                10000, 10000, 1, 0.01, 200, 20);
        homeGateway.setParentId(cloud.getId());
        homeGateway.setUplinkLatency(50.0);
        fogDevices.add(homeGateway);
        Log.printLine("Fog Node created: " + homeGateway.getName());

        FogDevice smartThermostat = createFogDevice("Smart-Thermostat", 1500, 2048,
                10000, 10000, 2, 0.0, 87.53, 82.44);
        smartThermostat.setParentId(homeGateway.getId());
        smartThermostat.setUplinkLatency(10.0);
        fogDevices.add(smartThermostat);
        Log.printLine("Edge Node created: " + smartThermostat.getName());

        Sensor temperatureSensor = new Sensor("temp-sensor-1", "TEMPERATURE",
                broker.getId(), appId, new DeterministicDistribution(2.0));
        temperatureSensor.setGatewayDeviceId(smartThermostat.getId());
        temperatureSensor.setLatency(1.0);
        sensors.add(temperatureSensor);
        Log.printLine("Temperature Sensor created and configured");

        Actuator hvacController = new Actuator("hvac-controller-1", broker.getId(), appId, "HVAC_CONTROL");
        hvacController.setGatewayDeviceId(homeGateway.getId());
        hvacController.setLatency(1.5);
        actuators.add(hvacController);
        Log.printLine("HVAC Controller Actuator created");

        Actuator alertNotifier = new Actuator("alert-notifier-1", broker.getId(), appId, "ALERT_NOTIFICATION");
        alertNotifier.setGatewayDeviceId(homeGateway.getId());
        alertNotifier.setLatency(0.5);
        actuators.add(alertNotifier);
        Log.printLine("Alert Notification Actuator created");

        Application app = createTemperatureMonitoringApp(appId, broker.getId());
        app.setUserId(broker.getId());
        Log.printLine("Application DAG created with modules and edges");

        ModuleMapping mapping = ModuleMapping.createModuleMapping();
        mapping.addModuleToDevice("TemperatureProcessor", "Smart-Thermostat");
        mapping.addModuleToDevice("AlertManager", "Home-Gateway");
        mapping.addModuleToDevice("CloudAnalyzer", "cloud");
        Log.printLine("Module mapping configured");

        Controller controller = new Controller("smart-home-controller", fogDevices, sensors, actuators);
        controller.submitApplication(app,
                new ModulePlacementEdgewards(fogDevices, sensors, actuators, app, mapping));

        TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

        Log.printLine("Starting Smart Home Temperature Monitoring Simulation...");
        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        printComprehensiveReport(app);

        Log.printLine("=== Smart Home Temperature Monitoring System Completed ===");
    }

    private static Application createTemperatureMonitoringApp(String appId, int userId) {
        Application app = Application.createApplication(appId, userId);

        app.addAppModule("TemperatureProcessor", 10);
        app.addAppModule("AlertManager", 10);
        app.addAppModule("CloudAnalyzer", 20);

        app.addAppEdge("TEMPERATURE", "TemperatureProcessor",
                500, 500, "TEMPERATURE", org.fog.entities.Tuple.UP, AppEdge.SENSOR);

        app.addAppEdge("TemperatureProcessor", "AlertManager",
                1000, 500, "TEMP_EVENT", org.fog.entities.Tuple.UP, AppEdge.MODULE);

        app.addAppEdge("TemperatureProcessor", "CloudAnalyzer",
                2000, 1000, "TEMP_ANALYTICS", org.fog.entities.Tuple.UP, AppEdge.MODULE);

        app.addAppEdge("TemperatureProcessor", "HVAC_CONTROL",
                200, 200, "HVAC_CMD", org.fog.entities.Tuple.DOWN, AppEdge.ACTUATOR);

        app.addAppEdge("AlertManager", "ALERT_NOTIFICATION",
                100, 100, "ALERT", org.fog.entities.Tuple.DOWN, AppEdge.ACTUATOR);

        app.addTupleMapping("TemperatureProcessor", "TEMPERATURE",
                "TEMP_EVENT", new FractionalSelectivity(0.3));

        app.addTupleMapping("TemperatureProcessor", "TEMPERATURE",
                "TEMP_ANALYTICS", new FractionalSelectivity(0.2));

        app.addTupleMapping("TemperatureProcessor", "TEMPERATURE",
                "HVAC_CMD", new FractionalSelectivity(0.5));

        app.addTupleMapping("AlertManager", "TEMP_EVENT",
                "ALERT", new FractionalSelectivity(1.0));

        AppLoop loop1 = new AppLoop(Arrays.asList("TemperatureProcessor", "CloudAnalyzer"));
        AppLoop loop2 = new AppLoop(Arrays.asList("TemperatureProcessor", "AlertManager", "ALERT_NOTIFICATION"));
        AppLoop loop3 = new AppLoop(Arrays.asList("TemperatureProcessor", "HVAC_CONTROL"));

        List<AppLoop> loops = new ArrayList<>();
        loops.add(loop1);
        loops.add(loop2);
        loops.add(loop3);
        app.setLoops(loops);

        return app;
    }

    private static FogDevice createFogDevice(String name, long mips, int ram,
                                             long upBw, long downBw, int level,
                                             double ratePerMips,
                                             double busyPower, double idlePower) throws Exception {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<PowerHost> hostList = new ArrayList<>();
        hostList.add(host);

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0,
                3.0, 0.05, 0.001, 0.0);

        FogDevice device = new FogDevice(
                name,
                characteristics,
                new AppModuleAllocationPolicy(hostList),
                new LinkedList<Storage>(),
                10,
                upBw,
                downBw,
                0,
                ratePerMips
        );
        device.setLevel(level);
        return device;
    }

    private static void printComprehensiveReport(Application app) {
        System.out.println("\n================= REPORT =================");

        TimeKeeper tk = TimeKeeper.getInstance();
        int idx = 1;
        for (Integer loopId : tk.getLoopIdToCurrentAverage().keySet()) {
            Double avg = tk.getLoopIdToCurrentAverage().get(loopId);
            System.out.printf("Loop %d average latency : %.3f ms%n",
                    idx, avg == null ? Double.NaN : avg);
            idx++;
        }

        double totalEnergy = 0.0;
        for (FogDevice d : fogDevices) {
            totalEnergy += d.getEnergyConsumption();
        }

        System.out.printf("Total Energy Consumption : %.3f J%n", totalEnergy);
        System.out.println("=========================================\n");
    }
}
