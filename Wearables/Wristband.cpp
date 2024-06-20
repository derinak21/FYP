#include <Wire.h>
#include "MAX30105.h"
#include <PeakDetection.h>
#include "KickFiltersRT.h"
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include <DFRobot_ENS160.h>
#include <Arduino.h>
#include <vector>
#include <WiFi.h>
#include <PubSubClient.h>


const char* ssid = "iPhone";
const char* password = "derin2001";
bool monitor = false;

const char* mqtt_server = "18.169.68.55";
const int mqtt_port = 1883;
const char* mqtt_user = "da621";
const char* mqtt_password = "derin2001";
const char* mqtt_topic = "monitor";
WiFiClient espClient;
PubSubClient mqttClient(espClient);



DFRobot_ENS160_I2C ENS160(&Wire1, /*I2CAddr*/ 0x53);



Adafruit_BME280 bme; // I2C
volatile float BME_T = 0;
volatile float BME_RH = 0;
volatile float ENS_TVOC = 0;
volatile float ENS_CO2 = 0;
std::vector<unsigned long> bpmValues;

MAX30105 particleSensor;
PeakDetection peakDetection;
KickFiltersRT<float> filtersRT;
#define debug Serial //Uncomment this line if you're using an Uno or ESP
//#define debug SerialUSB //Uncomment this line if you're using a SAMD21
const float fs = 100;
const int BUFFER_SIZE = 10;


void callback(char* topic, byte* payload, unsigned int length) {
//  Serial.print("Message arrived [");
//  Serial.print(topic);
//  Serial.print("] ");
//  for (int i = 0; i < length; i++) {
//    Serial.print((char)payload[i]);
//  }
//  Serial.println();

  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  if (message.equals("Monitor Start")) {
    monitor = true;
    Serial.println("Start signal received.");
  } else if (message.equals("Monitor Stop")) {
    monitor = false;
    Serial.println("Stop signal received.");
  }
  

}

void reconnect() {
  // Loop until we're reconnected
  while (!mqttClient.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    if (mqttClient.connect("ESP32Client2", mqtt_user, mqtt_password)) {
      Serial.println("connected");
      mqttClient.subscribe("da621/monitor"); 

    } else {
      Serial.print("failed, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}




void setup()
{
  debug.begin(9600);
  debug.println("MAX30105 Basic Readings Example");
  delay(2000);
  Wire1.setPins(SDA1, SCL1);
  Wire1.begin();
  delay(2000);


  if(!bme.begin(0x77, &Wire1)) {
    Serial.println("BME280 initialization failed!");
    while (1); // halt the program
  } else {
    Serial.println("BME280 initialized successfully!");
  }

  bme.setSampling(Adafruit_BME280::MODE_NORMAL,
                  Adafruit_BME280::SAMPLING_X4,   // temperature
                  Adafruit_BME280::SAMPLING_NONE, // pressure off
                  Adafruit_BME280::SAMPLING_X4,   // humidity
                  Adafruit_BME280::FILTER_OFF,
                  Adafruit_BME280::STANDBY_MS_0_5 );


  while( NO_ERR != ENS160.begin() ){
    Serial.println("Communication with device failed, please check connection");
    delay(3000);
  }
  Serial.println("Begin ok!");
  ENS160.setPWRMode(ENS160_STANDARD_MODE);
  ENS160.setTempAndHum(/*temperature=*/25.0, /*humidity=*/50.0);
  
  particleSensor.begin(Wire1, 400000, 0x57);
  // Initialize sensor
  while (particleSensor.begin(Wire1, 400000, 0x57) == false)
  {
    debug.println("MAX30105 was not found. Please check wiring/power. ");
    delay(1000);
  }
   debug.println("MAX30105 was found.");
  particleSensor.setup(0x1F, 4, 2, 100, 411, 16384); // Example settings

   peakDetection.begin(20, 3, 0.6);               // sets the lag,F threshold and influence



  WiFi.begin(ssid, password);

  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  // Connected to Wi-Fi
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(callback);
  mqttClient.connect("ESP32Client2", mqtt_user, mqtt_password);
  while (!mqttClient.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    if (mqttClient.connect("ESP32Client2", mqtt_user, mqtt_password)) {
      Serial.println("connected");
      mqttClient.subscribe("da621/monitor"); 

    } else {
      Serial.print("failed, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
  mqttClient.subscribe("da621/monitor"); 


}
unsigned long lastSensorReadTime = 0;

float last=0;
float last2=0;
float inter =3000;
bool sign= false; 
float lasttime =0;
int count = 0; 
float lastt =0;
double lastbpm = 0;
double lasttime2 = 0;
double la = 0;

bool notbeat = false;
void loop()
{

if(monitor){

   double lpfiltered = filtersRT.lowpass(particleSensor.getIR(), 4, fs);

    double hpfiltered = filtersRT.highpass(lpfiltered, 1, fs);

    double difference = hpfiltered- last;
//    debug.println(difference);

    last = hpfiltered;
    double squared = difference*difference;
    peakDetection.add(squared);                     // adds a new data point
    double moving = peakDetection.getFilt();
    int peak = peakDetection.getPeak();
    if(peak ==1 && inter>2000){
       lasttime = millis();
  //     debug.println(moving+100);
  
    }
    if((difference>0)){
      if(notbeat&&(millis()-la)>500){
  //       debug.println(difference+100);
         float bpm = 60000/(millis()-la);
         la= millis();
  //       debug.println(bpm);
         bpmValues.push_back(bpm);
         if (bpmValues.size() == 5) {
          float sumBPM = 0;
          for (float bpm : bpmValues) {
            sumBPM += bpm;
          }
          float averageBPM = sumBPM / bpmValues.size();
          debug.print("average BPM over last 5 beats: ");
          debug.println(averageBPM);
          bpmValues.clear();
          BME_T = bme.readTemperature();
          BME_RH = bme.readHumidity();
        
          // Print the temperature and humidity readings
          Serial.print("Temperature: ");
          Serial.print(BME_T);
          Serial.println(" °C");
          Serial.print("Humidity: ");
          Serial.print(BME_RH);
          Serial.println(" %");
        
          uint8_t AQI = ENS160.getAQI();
          Serial.print("Air quality index : ");
          Serial.println(AQI);
        
          /**
           * Get TVOC concentration
           * Return value range: 0–65000, unit: ppb
           */
          uint16_t TVOC = ENS160.getTVOC();
          Serial.print("Concentration of total volatile organic compounds : ");
          Serial.print(TVOC);
          Serial.println(" ppb");
        
          /**
           * Get CO2 equivalent concentration calculated according to the detected data of VOCs and hydrogen (eCO2 – Equivalent CO2)
           * Return value range: 400–65000, unit: ppm
           * Five levels: Excellent(400 - 600), Good(600 - 800), Moderate(800 - 1000), 
           *               Poor(1000 - 1500), Unhealthy(> 1500)
           */
          uint16_t ECO2 = ENS160.getECO2();
          Serial.print("Carbon dioxide equivalent concentration : ");
          Serial.print(ECO2);
          Serial.println(" ppm");
  
          
          String data = "da621," + String(BME_T) + ","
              + String(BME_RH) + "," + String(AQI) + ","
              + String(TVOC) + "," + String(ECO2) + "," + String(averageBPM);
  
            mqttClient.publish("sensordata_wrist", data.c_str());
          
  
        }
  
      }
      else{
  //      debug.println(difference);
  
      }
      notbeat = false;
  
    }
    else{
      notbeat=true;
  //      debug.println(difference);
  
    }
  
  
    
    double difference2 = (moving-last2)*100;
    last2= moving;
  
  }
  mqttClient.loop();

}