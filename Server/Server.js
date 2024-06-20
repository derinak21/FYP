const express = require("express");
const bodyParser = require("body-parser");
const fs = require("fs");
const https = require("https");
const bcrypt = require("bcrypt");
const AWS = require("aws-sdk");
const uuid = require('uuid');
const path = require('path');
const aedes = require('aedes')();
const server = require('net').createServer(aedes.handle);
const { exec } = require('child_process');

const app = express();
const mqttPort = 1883;
let start_stop = false;
let send_signal = false;
 


server.listen(mqttPort, function () {
    console.log('MQTT server listening on port', mqttPort);
});


function sendStopSignal() {
    let signal;
    if(start_stop){
        signal = 'Start';
    }
    else{
        signal = 'Stop';
    }
    console.log('Control signal', signal);

    const controlTopic = 'control';
    console.log(signal)
    aedes.publish({ topic: controlTopic, payload: Buffer.from(signal) });
}

aedes.on('client', function (client) {
    console.log('Client connected:', client.id);
});


aedes.subscribe('#', function (packet, cb) {
    console.log('Received message on topic:', packet.topic);
    cb(); 
});

aedes.on('publish', function (packet, client, done) {
    console.log('Received publish');
    if (packet.topic === 'iot') {
        console.log('Received iot');
        const sensorData = packet.payload.toString();
        console.log('Received Sensor Data:', sensorData);
      
    }
});


app.use(bodyParser.json());

AWS.config.update({ region: 'eu-west-2' });

const dynamoDB = new AWS.DynamoDB.DocumentClient();

const tableName = 'App';

const privateKeyPath = path.join(__dirname, 'private-key.pem');
const privateKey = fs.readFileSync(privateKeyPath);
const certificatePath = path.join(__dirname, 'certificate.pem');
const certificate = fs.readFileSync(certificatePath);

const sslOptions = {
    key: privateKey,
    cert: certificate,
};

app.get("/", (req, res) => {
    console.log("server reached");
    res.send("Hello, this is the root route!");
});

app.get("/test", (req, res) => {
    res.json({ success: true, message: "Server connectivity test successful" });
});

const userSchema = {
    TableName: tableName,
    KeySchema: [
        { AttributeName: 'email', KeyType: 'HASH' }
    ],
    AttributeDefinitions: [
        { AttributeName: 'email', AttributeType: 'S' },
        { AttributeName: 'password', AttributeType: 'S' },
        { AttributeName: 'gender', AttributeType: 'S' },
        { AttributeName: 'age', AttributeType: 'S' },
        { AttributeName: 'medical', AttributeType: 'S' },
        { AttributeName: 'smoker_status', AttributeType: 'S' },
        { AttributeName: 'triggers', AttributeType: 'S' },
        { AttributeName: 'deviceid', AttributeType: 'S' },
        { AttributeName: 'restingheartrate', AttributeType: 'S' }
    ],
    ProvisionedThroughput: {
        ReadCapacityUnits: 5,
        WriteCapacityUnits: 5
    }
};

app.post("/signup", async (req, res) => {
    try {
        console.log("signup request");
        const { email, password } = req.body;

        const userExists = await getUserByEmail(email);
        if (userExists) {
            return res.status(200).json({ success: false, message: "Account already exists", user: null });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        const user = {
            email,
            password: hashedPassword,
        };

        await createUser(user);

        res.status(200).json({ success: true, message: "User registered", user: user });

    } catch (error) {
        console.error("Error in signup:", error);
        res.status(400).json({ success: false, message: error.message, user: null });
        console.log("error in signup");
    }
});

app.post("/medicalinfo", async (req, res) => {
    try {
        console.log("medical request");
        const { email, name, age, gender, medical, smoker_status, triggers, deviceid, restingheartrate } = req.body;
        const additional_info = { name: name, age: age, gender: gender, medical: medical, smoker_status: smoker_status, triggers: triggers, deviceid: deviceid, restingheartrate: restingheartrate };

        const updatedUser = await updateUser(email, additional_info);

        if (updatedUser) {
            console.log('User updated successfully:', updatedUser);
            res.status(200).json({ success: true, message: "User updated successfully", user: updatedUser });
        } else {
            console.error('User not found');
            res.status(200).json({ success: false, message: "User not found", user: null });
        }

    } catch (error) {
        console.error('Error updating user:', error);
        res.status(500).json({ success: false, message: "Error updating user", user: null });
    }
});

app.post("/login", async (req, res) => {
    console.log("login request");
    try {
        const { email, password } = req.body;

        const user = await getUserByEmail(email);

        if (!user) {
            res.status(200).json({ success: false, message: "Account does not exist", user: null });
        } else {
            const isPasswordValid = await bcrypt.compare(password, user.password);

            if (!isPasswordValid) {
                res.status(200).json({ success: false, message: "Wrong password", user: null });
            } else {
                res.status(200).json({ success: true, message: "Login successful", user: user });
            }
        }

    } catch (error) {
        res.status(400).json({ success: false, error: error.message, user: user });
    }
});


app.post("/start_stop", async (req, res) => {
    console.log("start/stop IOT");
    try {
        const { signal } = req.body;
        start_stop = signal;
        if(start_stop){
            console.log("start signal received");
        }
        else{
            console.log("start signal received");
        }

        sendStopSignal()
        res.status(200).json({ success: true, message: "Starting/stopping now", user: null });


    } catch (error) {
        res.status(400).json({ success: false, error: error.message, user: user });
    }
});



const HTTPS_PORT = 8443;
const HOST = "0.0.0.0";

const httpsServer = https.createServer(sslOptions, app);

httpsServer.listen(HTTPS_PORT, HOST, () => {
    console.log(`HTTPS server is listening on https://localhost:${HTTPS_PORT}`);
});

async function getUserByEmail(email) {
    const params = {
        TableName: tableName,
        Key: {
            email: email
        }
    };

    try {
        const result = await dynamoDB.get(params).promise();
        return result.Item;
    } catch (error) {
        console.error("Error getting user:", error);
        throw error;
    }
}

async function createUser(user) {
    const params = {
        TableName: tableName,
        Item: user
    };

    try {
        await dynamoDB.put(params).promise();
    } catch (error) {
        console.error("Error creating user:", error);
        throw error;
    }
}

async function updateUser(email, additional_info) {
    const params = {
        TableName: tableName,
        Key: {
            email: email
        },
        UpdateExpression: 'SET #name = :name, #age = :age, #gender = :gender, #medical = :medical, #smoker_status = :smoker_status, #triggers = :triggers, #deviceid = :deviceid, #restingheartrate = :restingheartrate',
        ExpressionAttributeNames: {
            '#name': 'name',
            '#age': 'age',
            '#gender': 'gender',
            '#medical': 'medical',
            '#smoker_status': 'smoker_status',
            '#triggers' : 'triggers',
            '#deviceid' : 'deviceid',
            '#restingheartrate' : 'restingheartrate'


        },
        ExpressionAttributeValues: {
            ':name': additional_info.name,
            ':age': additional_info.age,
            ':gender': additional_info.gender,
            ':medical': additional_info.medical,
            ':smoker_status': additional_info.smoker_status,
            ':triggers': additional_info.triggers,
            ':deviceid': additional_info.deviceid,
            ':restingheartrate': additional_info.restingheartrate,

        },
        ReturnValues: 'ALL_NEW'
    };

    try {
        const result = await dynamoDB.update(params).promise();
        return result.Attributes;
    } catch (error) {
        console.error("Error updating user:", error);
        throw error;
    }
}
