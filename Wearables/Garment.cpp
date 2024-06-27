// ESP32 Frequency Meter
// Code by Kristel Fobelets and Kris Thielemans 11/2/2023
// Adapted from:
// https://blog.eletrogate.com/esp32-frequencimetro-de-precisao
// Rui Viana and Gustavo Murta august/2020
// pins https://github.com/espressif/arduino-esp32/blob/master/variants/esp32s2/pins_arduino.h
// if the board disappears from Tools then follow the steps described in:
// https://blog.espressif.com/arduino-for-esp32-s2-and-esp32-c3-is-coming-f36d79967eb8

/* set pulse counters for the ESP32 WROOM 32D collpit oscillator implementation
//#define PCNT_INPUT_SIG_IO_0     GPIO_NUM_26                               
//#define PCNT_INPUT_CTRL_IO_0    GPIO_NUM_32                               
//#define PCNT_INPUT_SIG_IO_1     GPIO_NUM_27                              
//#define PCNT_INPUT_CTRL_IO_1    GPIO_NUM_33                             
//#define PCNT_H_LIM_VAL          overflow

Wire.begin(); //Join the bus as controller. 
  //By default .begin() will set I2C SCL to Standard Speed mode of 100kHz
  Wire.setClock(400000); //Optional - set I2C SCL to High Speed Mode of 400kHz
*/

#include "stdio.h"                                                        // Library STDIO
#include "FS.h"
#include "SD.h"
#include "SPI.h"
#include "driver/pcnt.h"                                                  // Library ESP32 PCNT
#include "soc/pcnt_struct.h"
#include <vector>
#include <numeric> 

#include "Adafruit_MPU6050.h"
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include "ScioSense_ENS160.h"
#include <PeakDetection.h>
#include <WiFiManager.h>          
PeakDetection peakDetection;
WiFiManager wifiManager;

void initTimers();
void init_PCNT(pcnt_unit_t unit, int pcnt_input_sig_io, int pcnt_input_ctrl_io);
void init_frequencyMeter();
void read_data();
void read_counters();
void start_counters();
void read_PCNT(void *);
TaskHandle_t Task1;
void Task1code( void* parameter);
Adafruit_BME280 bme; // I2C
ScioSense_ENS160 ens160(ENS160_I2CADDR_1);
Adafruit_MPU6050 mpu;
unsigned long globalMillis;
void setup_wifi();
SPIClass spi = SPIClass(2);
File output;
const char * const SD_filename = "/derinakoutputfile2.csv";
void callback(char* topic, byte* payload, unsigned int length);
#define PCNT_COUNT_UNIT_0       PCNT_UNIT_0                              // Set Pulse Counter Unit
#define PCNT_COUNT_UNIT_1       PCNT_UNIT_1                              // Set Pulse Counter Unit
#define PCNT_COUNT_CHANNEL      PCNT_CHANNEL_0                           // Set Pulse Counter channel
void reconnect();
#define PCNT_INPUT_SIG_IO_0     GPIO_NUM_34                               // Set Pulse Counter input - Freq Meter Input
#define PCNT_INPUT_CTRL_IO_0    GPIO_NUM_32                               // Set Pulse Counter Control GPIO pin - HIGH = count up, LOW = count down  
#define PCNT_INPUT_SIG_IO_1     GPIO_NUM_35                              // Set Pulse Counter input - Freq Meter Input
#define PCNT_INPUT_CTRL_IO_1    GPIO_NUM_33                             // Set Pulse Counter Control GPIO pin - HIGH = count up, LOW = count down  
#define PCNT_H_LIM_VAL          overflow                                 // Overflow of Pulse Counter 

volatile bool   flag            = true;                                  // Flag to enable print frequency reading
const uint32_t  overflow        = 32000;                                 // Max Pulse Counter value 32000
const uint32_t  sample_time     = 10000;                                 // sample time in microseconds to count pulses
int             print_counter   = 0;                                     // count when we store results
const int       print_every     = 1;                                    // how many samples we skip before storing
volatile float  esp_time      = 0;                                     // time elapsed in microseconds since boot
volatile float  esp_time_interval = 0;                                 // actual time between 2 samples (should be close to sample_time)
volatile float  frequency_0   = 0;                                     // frequency value
volatile float  frequency_1   = 0;                                     // frequency value
const int windowSize = 20;
float frequencyReadings[windowSize]; // Array to store last 10 frequency readings
int currentIndex = 0; // Index to keep track of the current position in the array
unsigned long lastExecutionTime = 0;
unsigned long executionCounter = 0;
portMUX_TYPE timerMux = portMUX_INITIALIZER_UNLOCKED;                    // portMUX_TYPE to do synchronisation



#include <WiFi.h>
#include <PubSubClient.h>

const char* ssid = "iPhone";
const char* password = "derin2001";
bool control = false;
bool monitor = false;

const char* mqtt_server = "18.169.68.55";
const int mqtt_port = 1883;
const char* mqtt_user = "da621";
const char* mqtt_password = "derin2001";
const char* mqtt_topic = "control";
const char* mqtt_topic2 = "monitor";

WiFiClient espClient;
PubSubClient mqttClient(espClient);

void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {

  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  if (message.equals("Start")) {
    control = true;
    Serial.println("Start signal received.");
  } else if (message.equals("Stop")) {
    control = false;
    Serial.println("Stop signal received.");
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
  while (!mqttClient.connected()) {
    Serial.print("Attempting MQTT connection...");
    if (mqttClient.connect("ESP32Client", mqtt_user, mqtt_password)) {
      Serial.println("connected");
      mqttClient.subscribe("da621/control"); 
      mqttClient.subscribe("da621/monitor"); 

    } else {
      Serial.print("failed, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}


//----------------------------------------------------------------------------------------
void setup()
{
  Serial.begin(115200);                                                   // Serial Console Arduino 115200 Bps
  spi.begin(); 
  init_frequencyMeter();                                                 // Initialize Frequency Meter
  peakDetection.begin(60, 3, 0.6);               // sets the lag,F threshold and influence

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

  setup_wifi();
  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(callback);
  mqttClient.connect("ESP32Client", mqtt_user, mqtt_password);
  mqttClient.subscribe("da621/control"); 
  mqttClient.subscribe("da621/monitor"); 

}



#define WINDOW_SIZE 20  // Define the size of the window
#define BUFFER_SIZE 60


int count=0;
bool isHoldingBreath = false; 
int lastValue = 0;         
std::vector<int> fifoBuffer;
double lastfreq = 0;         
double lastdiff=0;
unsigned long lastPeakTime = 0; 
unsigned long lastPeakTime2 = 0; 
unsigned long cooldownPeriod = 1500; 

void read_data()
{

    executionCounter++;
    unsigned long currentTime = millis();

    if (currentTime - lastExecutionTime >= 1000) {
        executionCounter = 0;
        lastExecutionTime = currentTime;
    }

    unsigned long c = millis();    
  if (abs(-frequency_0 - lastValue) <= 15) {
      peakDetection.add(-frequency_0);                    
      double filtered = peakDetection.getFilt();  
      
    
    currentIndex = (currentIndex + 1) % BUFFER_SIZE;
    int diff = (filtered - lastfreq) > 0 ? 1 : 0;
    lastfreq = filtered;
    fifoBuffer.push_back(diff); 

    int lastcurrent = currentIndex- 10;
    if (lastcurrent < 0){
        lastcurrent = lastcurrent + BUFFER_SIZE; 
    }

  if (difference < 2) {


    if (!isHoldingBreath) {
        isHoldingBreath = true;
        unsigned long currentMillis = millis();
        char message[100]; 
        if(control){

        }
       
    } 
  } else {
      if (isHoldingBreath) {
        isHoldingBreath = false;
        unsigned long currentMillis = millis(); 
        char message[100]; 
        if(control){

        }
      }
  }
      int sum1=0;
      int sum2=0;
      for(int i=0; i<5; i++){
        sum1 += fifoBuffer[i];
        sum2 += fifoBuffer[i+5];
      }

    lastdiff= diff;
        if (sum1 < 3 && sum2 > 2 && (millis() - lastPeakTime) > cooldownPeriod) {
            lastPeakTime = millis();     
            if(!isHoldingBreath){
              char message[100]; 
              if(control){
                sprintf(message, "Minima - Timestamp: %lu", lastPeakTime); 
                mqttClient.publish("da621/iot", message); 
              } 
            }
        }
    
        if (sum2 < 3 && sum1 > 2 && (millis() - lastPeakTime2) > cooldownPeriod && (millis() - lastPeakTime) > 500) {
            if(!isHoldingBreath){
              char message[100]; 
              if(control){
                sprintf(message, "Maxima - Timestamp: %lu", lastPeakTime2); 
                mqttClient.publish("da621/iot", message); 
              }
              else if(monitor){
                float monitordiff = millis()-lastPeakTime2;
                float rr = 60000/monitordiff;
                char rrStr[10]; 
                dtostrf(rr, 6, 2, rrStr); 
                char message[50]; 
                sprintf(message, "da621,%s", rrStr); 
                Serial.println(message); 
                mqttClient.publish("da621/iot", message); 
              }
              lastPeakTime2 = millis();        

            }
        }
    
    if (fifoBuffer.size() > 10) {
        fifoBuffer.erase(fifoBuffer.begin());
    }
    }
  lastValue = -frequency_0;    
}






//----------------------------------------------------------------------------------
void init_PCNT(pcnt_unit_t unit, int pcnt_input_sig_io, int pcnt_input_ctrl_io)                                                      // Initialize and run PCNT unit
{
  pcnt_config_t pcnt_config = { };                                        // PCNT unit instance

  pcnt_config.pulse_gpio_num = pcnt_input_sig_io;                         // Pulse input GPIO - Freq Meter Input
  pcnt_config.ctrl_gpio_num = pcnt_input_ctrl_io;                         // Control signal input GPIO
  pcnt_config.unit = unit;                                                // Counting unit PCNT - 0
  pcnt_config.channel = PCNT_COUNT_CHANNEL;                               // PCNT unit number - 0
  pcnt_config.counter_h_lim = PCNT_H_LIM_VAL;                             // Maximum counter value
  pcnt_config.pos_mode = PCNT_COUNT_INC;                                  // PCNT positive edge count mode - inc
  pcnt_config.lctrl_mode = PCNT_MODE_DISABLE;                             // PCNT low control mode - disable
  pcnt_config.hctrl_mode = PCNT_MODE_KEEP;                                // PCNT high control mode - won't change counter mode
  pcnt_unit_config(&pcnt_config);                                         // Initialize PCNT unit

  pcnt_counter_pause(unit);                                               // Pause PCNT unit
  pcnt_counter_clear(unit);                                               // Clear PCNT unit

  pcnt_event_enable(unit, PCNT_EVT_H_LIM);                                // Enable event to watch - max count
  pcnt_intr_enable(unit);                                                 // Enable interrupts for PCNT unit
}

//----------------------------------------------------------------------------------
void read_counters()                                                    // Read Pulse Counters
{
  pcnt_counter_pause(PCNT_COUNT_UNIT_0); 
  pcnt_counter_pause(PCNT_COUNT_UNIT_1); 
  const int64_t esp_time_now = esp_timer_get_time();
  int16_t pulses_0 = 0;
  int16_t pulses_1 = 0;
  pcnt_get_counter_value(PCNT_COUNT_UNIT_0, &pulses_0);                   // Read Pulse Counter value
  pcnt_get_counter_value(PCNT_COUNT_UNIT_1, &pulses_1);                   // Read Pulse Counter value
  frequency_0 = pulses_0  ;                                               // Calculation of frequency. 
  pcnt_counter_clear(PCNT_COUNT_UNIT_0);                                  // Clear Pulse Counter
  frequency_1 = pulses_1 ;                                                // Calculation of frequency. 
  pcnt_counter_clear(PCNT_COUNT_UNIT_1);                                  // Clear Pulse Counter
  esp_time_interval = esp_time_now - esp_time;
 }

void start_counters()
{
  esp_time = esp_timer_get_time();
  pcnt_counter_resume(PCNT_COUNT_UNIT_0); 
  pcnt_counter_resume(PCNT_COUNT_UNIT_1); 
}



void read_PCNT(void *)                                                    // Read Pulse Counter callback
{
  read_counters();
  //read_data();
  start_counters();
  flag = true;                                                            // Change flag to enable print
}
//---------------------------------------------------------------------------------
void init_frequencyMeter ()
{
  // Initialize and run PCNT units
  init_PCNT(PCNT_COUNT_UNIT_0, PCNT_INPUT_SIG_IO_0, PCNT_INPUT_CTRL_IO_0);
  init_PCNT(PCNT_COUNT_UNIT_1, PCNT_INPUT_SIG_IO_1, PCNT_INPUT_CTRL_IO_1);

  // create periodic timer for reading-out PCNTs
  esp_timer_create_args_t create_args;
  esp_timer_handle_t timer_handle;
  create_args.callback = read_PCNT;                                       // Set esp-timer argument
  esp_timer_create(&create_args, &timer_handle);                          // Create esp-timer instance
  esp_timer_start_periodic(timer_handle, sample_time);                    // Initialize High resolution timer (dependent on sampling time.
  start_counters();
}

//---------------------------------------------------------------------------------
void loop()
{
  if (control == true || monitor == true){
    if (flag == true)                                                     // If count has ended
    {
      flag = false;                                                       // change flag to disable print
      print_counter++;
      if (print_counter>=print_every)
      {
        print_counter = 0;
        read_data();
        
      }
    }
    
    delay(1); // units are millisecs
  }
    if (!mqttClient.connected()) {
      reconnect();
    }
    mqttClient.loop();
}

