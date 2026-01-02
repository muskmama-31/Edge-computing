//Assignment - 8

/* ================================================================================
NODE-RED EDGE COMPUTING ASSIGNMENT CODES
================================================================================
This file contains the Javascript code blocks required for the Function Nodes 
in each of the 4 assignment programs.
================================================================================
*/


/* ================================================================================
PROGRAM 1: Multi-Sensor Edge Anomaly Detection (Temperature + Humidity)
================================================================================
*/

// --- FUNCTION NODE 1: Data Generation ---
// Simulate sensor readings
let temp = 20 + Math.random() * 20; // Range: 20-40°C
let hum = 20 + Math.random() * 60;  // Range: 20-80%

// Round to 1 decimal place
temp = Math.round(temp * 10) / 10;
hum = Math.round(hum * 10) / 10;

// Set payload properties
msg.temperature = temp;
msg.humidity = hum;

msg.payload = {
    temperature: temp,
    humidity: hum,
    time: new Date().toISOString()
};

return msg;

// --- FUNCTION NODE 2: Anomaly Logic ---
let t = msg.temperature;
let h = msg.humidity;

let level = "NORMAL";
let message = "Status Normal";

// Apply Edge Logic Rules
if (t > 35 && h < 30) {
    level = "CRITICAL";
    message = "CRITICAL: Fire Risk";
} else if (t > 30 || h < 35) {
    level = "WARNING";
    message = "WARNING: Abnormal Conditions";
}

// Attach classification to message object for Switch Node
msg.level = level;
msg.message = message;

// Update payload with classification results
msg.payload = {
    temperature: t,
    humidity: h,
    level: level,
    message: message,
    time: new Date().toISOString()
};

return msg;


/* ================================================================================
PROGRAM 2: Edge-Based Predictive Maintenance Using Vibration Patterns
================================================================================
*/

// --- FUNCTION NODE: Simulate & Analyze ---

// 1. Simulate Vibration (0-100 units)
let vibration = Math.round(Math.random() * 100);

// 2. Retrieve past readings from context (memory)
// 'readings' is an array stored in the node's context
let readings = context.get('readings') || [];

// 3. Update Rolling Window (Keep last 10 readings)
readings.push(vibration);
if (readings.length > 10) {
    readings.shift();
}
// Save back to context
context.set('readings', readings);

// 4. Calculate Moving Average
let sum = readings.reduce((a, b) => a + b, 0);
let avg = sum / readings.length;

// 5. Detect Spike (Deviation > 25%)
let spike = false;
let deviationPercent = 0;

if (avg > 0) {
    deviationPercent = Math.abs(vibration - avg) / avg;
    if (deviationPercent > 0.25) {
        spike = true; // Spike Detected
    }
}

// 6. Output Construction
msg.movingAverage = avg;
msg.spike = spike;
msg.deviationPercent = deviationPercent;

msg.payload = {
    currentVibration: vibration,
    movingAverage: avg.toFixed(2),
    spike: spike,
    deviationPercent: (deviationPercent * 100).toFixed(1) + "%",
    time: new Date().toISOString()
};

return msg;


/* ================================================================================
PROGRAM 3: Smart City Traffic Flow – Edge Counting & Congestion
================================================================================
*/

// --- FUNCTION NODE A: Vehicle Counter (Connect to Sensor Simulation Inject) ---

// Get current count from flow context (default 0)
// Using 'flow' context allows sharing data between nodes in the same tab
let count = flow.get('vehicleCount') || 0;

// Increment count (Simulate 1 vehicle passing)
count += 1;

// Store updated count back to flow context
flow.set('vehicleCount', count);

// Stop message here (Edge processing only, do not flood cloud)
return null;


// --- FUNCTION NODE B: Congestion Aggregator (Connect to 60s Interval Inject) ---

// Retrieve the count accumulated over the last minute
let count = flow.get('vehicleCount') || 0;
let level = "";

// Classification Rules
if (count < 20) {
    level = "Low Traffic";
} else if (count >= 20 && count < 50) {
    level = "Moderate Traffic";
} else {
    level = "High Congestion";
}

// Prepare Payload for Dashboard/Cloud
msg.payload = {
    trafficLevel: level,
    vehicleCountLastMinute: count,
    time: new Date().toISOString()
};

// Reset counter for the next minute cycle
flow.set('vehicleCount', 0);

return msg;


/* ================================================================================
PROGRAM 4: Edge Device for Smart Grid Load Balancing
================================================================================
*/

// --- FUNCTION NODE: Load Aggregation & Analysis ---

// Simulate 3 household loads in kW
// Example range: 0.0 to 4.0 kW each
let h1 = +(Math.random() * 4).toFixed(2);
let h2 = +(Math.random() * 4).toFixed(2);
let h3 = +(Math.random() * 4).toFixed(2);

// Aggregate total area consumption
let total = +(h1 + h2 + h3).toFixed(2);

let level = "";

// Classification Rules
// Total < 5 kW -> Normal
// 5–8 kW (inclusive) -> High Load – Monitor
// > 8 kW -> CRITICAL – Possible Overload

if (total < 5) {
    level = "Normal";
} else if (total >= 5 && total <= 8) {
    level = "High Load – Monitor";
} else {
    level = "CRITICAL – Possible Overload";
}

// Attach info to msg for Switch Node routing
msg.level = level;

msg.payload = {
    household1: h1,
    household2: h2,
    household3: h3,
    totalLoad: total,
    level: level,
    time: new Date().toISOString()
};

return msg;