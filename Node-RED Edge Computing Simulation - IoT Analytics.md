//Assignment - 8


# Node-RED Edge Computing Simulation - IoT Analytics

## Overview

The **IoT Analytics & Edge Computing** project is a simulation developed using **Node-RED**. It demonstrates how Edge Computing principles—such as local data processing, anomaly detection, and data aggregation—can be implemented to optimize IoT networks. By processing data at the "edge" (the Node-RED flows), the system filters out noise and sends only critical insights to the "cloud" (the Debug console), thereby conserving bandwidth and reducing latency.

## Table of Contents

1. [Problem Statement](https://www.google.com/search?q=%23problem-statement)
2. [Solution Architecture](https://www.google.com/search?q=%23solution-architecture)
3. [Implementation Details](https://www.google.com/search?q=%23implementation-details)
4. [Features](https://www.google.com/search?q=%23features)
5. [Code Structure](https://www.google.com/search?q=%23code-structure)
6. [Running the Simulation](https://www.google.com/search?q=%23running-the-simulation)
7. [Expected Output](https://www.google.com/search?q=%23expected-output)
8. [Performance Analysis](https://www.google.com/search?q=%23performance-analysis)

---

## Problem Statement

In traditional IoT architectures, sending raw sensor data to the cloud is inefficient and costly. This simulation addresses four specific edge computing use cases:

1. **Multi-Sensor Anomaly Detection**: Identifying fire risks by correlating Temperature and Humidity locally.
2. **Predictive Maintenance**: Detecting motor faults via vibration spikes using rolling averages, without transmitting normal data.
3. **Smart City Traffic**: Aggregating vehicle counts into "Traffic Levels" to reduce reporting frequency.
4. **Smart Grid Load Balancing**: Monitoring household power usage to detect transformer overloads in real-time.

---

## Solution Architecture

### Node-RED Edge Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        DATA GENERATION                      │
│  ┌──────────────┐                                           │
│  │  Inject Node │ (Simulates IoT Sensors)                   │
│  │  (Interval)  │ -> Generates Random Raw Data              │
│  └──────┬───────┘                                           │
└─────────┼───────────────────────────────────────────────────┘
          │ Raw Data Stream (e.g., Temp=36°C, Hum=25%)
          ▼
┌─────────────────────────────────────────────────────────────┐
│                        EDGE PROCESSING                      │
│  ┌──────────────┐      ┌──────────────┐      ┌────────────┐ │
│  │ Function Node│ ───▶ │  Logic Rules │ ───▶ │ Filtering  │ │
│  └──────────────┘      └──────────────┘      └────────────┘ │
│   • Data Normalization  • Threshold Checks    • Route msgs  │
│   • Rolling Averages    • Classification      • Drop noise  │
│   • Aggregation                                             │
└─────────┼───────────────────────────────────────────────────┘
          │ Filtered Insights (e.g., "CRITICAL Alert")
          ▼
┌─────────────────────────────────────────────────────────────┐
│                        CLOUD / ACTION                       │
│  ┌──────────────┐                                           │
│  │  Debug Node  │ (Simulates Cloud Dashboard)               │
│  │  (Output)    │ -> Receives only actionable events        │
│  └──────────────┘                                           │
└─────────────────────────────────────────────────────────────┘

```

---

## Implementation Details

### Core Scenarios

#### 1. Multi-Sensor Anomaly Detection

The edge device monitors two variables simultaneously. Simple thresholding isn't enough; the *combination* of factors determines the risk.

* **Logic:** `IF Temp > 35 AND Hum < 30 THEN "CRITICAL"`
* **Edge Action:** Only transmits if status is WARNING or CRITICAL.

#### 2. Predictive Maintenance (Vibration)

To avoid sending 1Hz vibration data to the cloud, the edge calculates a **Moving Average** of the last 10 readings.

```javascript
// Rolling Average Logic
readings.push(new_value);
if (readings.length > 10) readings.shift();
let avg = sum / readings.length;

```

* **Logic:** `IF (Current - Avg) / Avg > 0.25 THEN "SPIKE DETECTED"`
* **Edge Action:** Discards normal vibrations; reports only spikes.

#### 3. Smart City Traffic Flow

Instead of reporting every single car (Event-Driven), the edge aggregates data over time (Time-Driven).

* **Logic:** Counts pulses (vehicles) for 60 seconds.
* **Classification:**
* `< 20`: Low Traffic
* `20 - 50`: Moderate Traffic
* `> 50`: High Congestion


* **Edge Action:** Resets counter every minute; sends one status update per minute.

#### 4. Smart Grid Load Balancing

Simulates a local transformer managing 3 households.

* **Logic:** `Total Load = H1 + H2 + H3`
* **Threshold:** `IF Total > 8kW THEN "CRITICAL OVERLOAD"`
* **Edge Action:** Logs trends locally; alerts cloud immediately on overload.

---

## Features

### ✅ **Local Intelligence**

* Data is processed where it is created.
* Complex logic (Moving Averages, Multi-variable correlation) runs on the simulated gateway.

### ✅ **Bandwidth Optimization**

* **Filtering:** "Normal" status messages are often suppressed.
* **Aggregation:** High-frequency events (cars passing) are converted to low-frequency summaries (traffic level).

### ✅ **Context Awareness**

* The system uses context variables (`flow.get`, `context.get`) to remember past states (previous vibration readings or vehicle counts), enabling trend analysis.

---

## Code Structure

### Flow Components

| Node Type | Purpose | Configuration |
| --- | --- | --- |
| **Inject** | Simulation triggers | Interval: 1s - 5s (Depending on scenario) |
| **Function** | Core Edge Logic | Contains the JavaScript logic for processing |
| **Switch** | Routing | Routes messages based on `msg.level` or `msg.spike` |
| **Debug** | Cloud Interface | Displays the final JSON payload |

### Key JavaScript Snippet (Traffic Counter)

```javascript
// Function Node B: Congestion Aggregator
let count = flow.get('vehicleCount') || 0;
let level = "Low Traffic";

if (count > 50) level = "High Congestion";

msg.payload = {
    trafficLevel: level,
    count: count
};
return msg;

```

---

## Running the Simulation

### Prerequisites

* **Node-RED**: Installed locally or via Docker.

### Execution Steps

1. **Open Node-RED**: Navigate to `http://localhost:1880`.
2. **Import Logic**: Copy the JavaScript code provided for the Function Nodes.
3. **Construct Flow**: Drag and drop `Inject`, `Function`, and `Debug` nodes. Connect them linearly.
4. **Deploy**: Click the "Deploy" button in the top right.
5. **Monitor**: Open the "Debug" tab (Ctrl+G, D) to see the filtered output.

---

## Expected Output

### Sample Console Output (Program 1: Fire Risk)

```json
{
  "temperature": 38.5,
  "humidity": 22.1,
  "level": "CRITICAL",
  "message": "CRITICAL: Fire Risk",
  "time": "2024-03-15T10:00:05.123Z"
}

```

*Note: You will NOT see "Normal" readings in the debug tab if the filter is working correctly.*

### Sample Console Output (Program 2: Vibration Spike)

```json
{
  "currentVibration": 85,
  "movingAverage": "45.20",
  "spike": true,
  "deviationPercent": "88.1%",
  "time": "2024-03-15T10:05:00.000Z"
}

```

---

## Performance Analysis

This simulation highlights the specific advantages of Edge Computing demonstrated in the labs:

### 1. Latency Reduction

* **Scenario:** Smart Grid & Fire Risk.
* **Impact:** Critical alerts (Overload/Fire) are identified in milliseconds within the Function node. Round-trip time to a cloud server is eliminated for these time-sensitive decisions.

### 2. Bandwidth Conservation

* **Scenario:** Traffic Flow & Vibration Monitoring.
* **Impact:** Instead of sending 60 packets per minute (1 per second), the Traffic Flow program sends 1 packet per minute. This represents a **98.3% reduction** in data transmission overhead.

### 3. Data Privacy

* **Scenario:** Smart Metering.
* **Impact:** Granular household usage data (exactly when you turn on the kettle) remains in the local `context`. Only the aggregated "Total Load" and critical alerts leave the edge, preserving user privacy.