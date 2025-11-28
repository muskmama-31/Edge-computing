package org.fog.healthsim;

import java.util.*;
import java.text.DecimalFormat;

public class SimpleFogClustering {
    private static List<FogDevice> fogDevices = new ArrayList<>();
    private static ClusterManager clusterManager = ClusterManager.getInstance();
    private static DecimalFormat df = new DecimalFormat("0.00");

    public static void main(String[] args) {
        System.out.println("=== Starting Simple Fog Clustering Simulation ===");
        try {
            createFogDevices();
            clusterManager.formClusters();
            printResults();
            System.out.println("=== Simulation Completed Successfully ===");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in simulation: " + e.getMessage());
        }
    }

    private static void createFogDevices() {
        System.out.println("\nCreating 20 Fog Devices...");
        Random random = new Random(42);

        for (int i = 0; i < 20; i++) {
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            double energy = 70 + random.nextDouble() * 30;
            double load = random.nextDouble() * 50;
            int mips = 1000 + random.nextInt(2000);

            FogDevice device = new FogDevice(
                    "FogDevice_" + i,
                    x, y, energy, load, mips);

            fogDevices.add(device);
            clusterManager.addDevice(device);

            System.out.println(
                    "Created " + device.name +
                            " at (" + df.format(x) + ", " + df.format(y) + ")" +
                            " - Energy: " + df.format(energy) + "%, Load: " +
                            df.format(load) + "%, MIPS: " + mips);
        }
    }

    private static void printResults() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CLUSTERING FORMATION RESULTS");
        System.out.println("=".repeat(80));
        System.out.println("Total Fog Devices: " + fogDevices.size());
        System.out.println("Total Clusters Formed: " + clusterManager.getClusterCount());
        System.out.println("\nDETAILED CLUSTER INFORMATION:");
        System.out.println("-".repeat(80));

        for (FogDevice device : fogDevices) {
            String role = device.isClusterHead ? "CLUSTER HEAD" : "Member";
            System.out.printf(
                    "%-15s | Cluster: %2d | %-12s | Location: (%6s, %6s) | Energy: %5s%% | Load: %5s%% | MIPS: %4d%n",
                    device.name,
                    device.clusterId,
                    role,
                    df.format(device.x),
                    df.format(device.y),
                    df.format(device.energy),
                    df.format(device.load),
                    device.mips);
        }

        System.out.println("\nCLUSTER HEAD SUMMARY:");
        System.out.println("-".repeat(80));
        List<FogDevice> clusterHeads = clusterManager.getClusterHeads();
        for (FogDevice head : clusterHeads) {
            System.out.println(
                    "Cluster " + head.clusterId + " Head: " + head.name +
                            " with " + head.clusterMembers.size() + " members" +
                            " | Avg Distance: " + df.format(calculateAvgDistance(head)) + " units");
        }

        printStatistics();
    }

    private static double calculateAvgDistance(FogDevice head) {
        if (head.clusterMembers.size() <= 1)
            return 0.0;
        double totalDistance = 0;
        int count = 0;
        for (FogDevice member : head.clusterMembers) {
            if (member != head) {
                totalDistance += head.calculateDistance(member);
                count++;
            }
        }
        return count > 0 ? totalDistance / count : 0;
    }

    private static void printStatistics() {
        System.out.println("\nCLUSTERING STATISTICS:");
        System.out.println("-".repeat(80));

        int totalDevices = fogDevices.size();
        int clusterHeads = 0;
        int maxClusterSize = 0;
        int minClusterSize = Integer.MAX_VALUE;
        double totalEnergy = 0;
        double totalLoad = 0;

        for (FogDevice device : fogDevices) {
            totalEnergy += device.energy;
            totalLoad += device.load;
            if (device.isClusterHead) {
                clusterHeads++;
                int clusterSize = device.clusterMembers.size();
                maxClusterSize = Math.max(maxClusterSize, clusterSize);
                minClusterSize = Math.min(minClusterSize, clusterSize);
            }
        }

        double avgEnergy = totalEnergy / totalDevices;
        double avgLoad = totalLoad / totalDevices;
        double avgClusterSize = clusterHeads > 0 ? (double) totalDevices / clusterHeads : 0.0;

        System.out.println("Number of Cluster Heads: " + clusterHeads);
        System.out.println("Average Cluster Size: " + df.format(avgClusterSize) + " devices");
        System.out.println("Largest Cluster: " + maxClusterSize + " devices");
        System.out.println("Smallest Cluster: " + (clusterHeads > 0 ? minClusterSize : 0) + " devices");
        System.out.println("Average Energy Level: " + df.format(avgEnergy) + "%");
        System.out.println("Average Load: " + df.format(avgLoad) + "%");

        double efficiency = calculateClusteringEfficiency();
        System.out.println("Clustering Efficiency Score: " + df.format(efficiency) + "/100");
    }

    private static double calculateClusteringEfficiency() {
        double score = 0.0;
        List<FogDevice> heads = clusterManager.getClusterHeads();
        if (heads.isEmpty())
            return 0.0;

        int totalDevices = fogDevices.size();
        int idealSize = totalDevices / heads.size();
        double balanceScore = 0.0;

        for (FogDevice head : heads) {
            int size = head.clusterMembers.size();
            double deviation = Math.abs(size - idealSize);
            balanceScore += (1 - deviation / (double) idealSize);
        }
        balanceScore = (balanceScore / heads.size()) * 50;

        double energyScore = 0.0;
        for (FogDevice head : heads) {
            energyScore += head.energy;
        }
        energyScore = (energyScore / heads.size()) * 0.5;

        return balanceScore + energyScore;
    }
}

class FogDevice {
    String name;
    double x, y;
    double energy;
    double load;
    int mips;
    boolean isClusterHead;
    int clusterId;
    List<FogDevice> clusterMembers;

    public FogDevice(String name, double x, double y, double energy, double load, int mips) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.load = load;
        this.mips = mips;
        this.isClusterHead = false;
        this.clusterId = -1;
        this.clusterMembers = new ArrayList<>();
    }

    public double calculateDistance(FogDevice other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double calculateWeight(double avgDistance) {
        double alpha = 0.5, beta = 0.3, gamma = 0.2;
        return (alpha * energy) - (beta * avgDistance) - (gamma * load);
    }

    public void addClusterMember(FogDevice device) {
        if (!clusterMembers.contains(device)) {
            clusterMembers.add(device);
        }
    }
}

class ClusterManager {
    private static ClusterManager instance;

    private List<FogDevice> allDevices;
    private Map<Integer, FogDevice> clusterHeads;
    private final double MAX_DISTANCE = 250.0;
    private int nextClusterId = 1;

    private ClusterManager() {
        this.allDevices = new ArrayList<>();
        this.clusterHeads = new HashMap<>();
    }

    public static ClusterManager getInstance() {
        if (instance == null) {
            instance = new ClusterManager();
        }
        return instance;
    }

    public void addDevice(FogDevice device) {
        allDevices.add(device);
    }

    public void formClusters() {
        System.out.println("\nForming clusters with " + allDevices.size() + " devices...");
        System.out.println("Maximum cluster distance: " + MAX_DISTANCE);

        List<FogDevice> unclustered = new ArrayList<>(allDevices);
        Random random = new Random(42);

        while (!unclustered.isEmpty()) {
            FogDevice tempHead = findDeviceWithHighestEnergy(unclustered);
            tempHead.clusterId = nextClusterId;
            unclustered.remove(tempHead);

            List<FogDevice> clusterMembers = new ArrayList<>();
            clusterMembers.add(tempHead);

            Iterator<FogDevice> iterator = unclustered.iterator();
            while (iterator.hasNext()) {
                FogDevice device = iterator.next();
                double distance = tempHead.calculateDistance(device);
                if (distance <= MAX_DISTANCE) {
                    clusterMembers.add(device);
                    device.clusterId = nextClusterId;
                    iterator.remove();
                    System.out.println(
                            " Added " + device.name + " to cluster " + nextClusterId +
                                    " (distance: " + String.format("%.2f", distance) + " units)");
                }
            }

            FogDevice electedHead = electClusterHead(clusterMembers);
            electedHead.isClusterHead = true;

            for (FogDevice member : clusterMembers) {
                electedHead.addClusterMember(member);
                if (member != electedHead) {
                    member.isClusterHead = false;
                }
            }

            clusterHeads.put(nextClusterId, electedHead);
            System.out.println(
                    "✓ Cluster " + nextClusterId + " formed: " + electedHead.name +
                            " (Head) with " + (clusterMembers.size() - 1) + " members");
            nextClusterId++;
        }
    }

    private FogDevice findDeviceWithHighestEnergy(List<FogDevice> devices) {
        return devices.stream()
                .max(Comparator.comparing(device -> device.energy))
                .orElse(devices.get(0));
    }

    private FogDevice electClusterHead(List<FogDevice> clusterMembers) {
        Map<FogDevice, Double> avgDistances = new HashMap<>();

        for (FogDevice device : clusterMembers) {
            double totalDistance = 0.0;
            int count = 0;
            for (FogDevice other : clusterMembers) {
                if (device != other) {
                    totalDistance += device.calculateDistance(other);
                    count++;
                }
            }
            double avgDistance = count > 0 ? totalDistance / count : 0;
            avgDistances.put(device, avgDistance);
        }

        return clusterMembers.stream()
                .max(Comparator.comparing(device -> device.calculateWeight(avgDistances.get(device))))
                .orElse(clusterMembers.get(0));
    }

    public FogDevice getClusterHeadForDevice(FogDevice device) {
        return clusterHeads.get(device.clusterId);
    }

    public List<FogDevice> getClusterHeads() {
        return new ArrayList<>(clusterHeads.values());
    }

    public int getClusterCount() {
        return clusterHeads.size();
    }
}
