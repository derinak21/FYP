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

void init_cardreader();
void initTimers();
void init_sensors();
void init_PCNT(pcnt_unit_t unit, int pcnt_input_sig_io, int pcnt_input_ctrl_io);
void init_frequencyMeter();
void write_header();
void read_data();
void read_counters();
void start_counters();
void write_data();
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
volatile float BME_T = 0;
volatile float BME_RH = 0;
volatile float ENS_TVOC = 0;
volatile float ENS_CO2 = 0;
volatile float ax_x = 0;
volatile float ax_y = 0;
volatile float ax_z = 0; 
unsigned long lastSensorReadTime = 0;
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
  // Loop until we're reconnected
  while (!mqttClient.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    if (mqttClient.connect("ESP32Client", mqtt_user, mqtt_password)) {
      Serial.println("connected");
      mqttClient.subscribe("control"); 
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


//----------------------------------------------------------------------------------------
void setup()
{
Serial.begin(115200);                                                   // Serial Console Arduino 115200 Bps
spi.begin(); 
init_cardreader();
init_frequencyMeter();                                                 // Initialize Frequency Meter
init_sensors();
  peakDetection.begin(60, 3, 0.6);               // sets the lag,F threshold and influence


//WiFiManager wifiManager;
//
//    // Try to connect to WiFi, if connection fails, start configuration portal
//    if (!wifiManager.autoConnect("AutoConnectAP")) {
//        Serial.println("Failed to connect and hit timeout");
//        // Reset and try again, or maybe put it to deep sleep
//        ESP.restart();
//        delay(1000);
//    }


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
  mqttClient.subscribe("control"); 
  mqttClient.subscribe("monitor"); 

}


void write_header()
{
 output = SD.open(SD_filename,FILE_APPEND);
output.println("t (esp timer), f0 (Hz), f1 (Hz), T (C), RH (%), VOC (ppb), CO2 (ppm), ax (m/s2), ay (m/s2), az (m/s2)"); //will only work at 1s sampling time
// output.println("t (esp timer), f0 (Hz), f1 (Hz), T (C), RH (%), ax (m/s2), ay (m/s2), az (m/s2)");
 output.close();
}

void init_cardreader()
{
  if (!SD.begin()) {
    Serial.println("Card Mount Failed");
    return;
  }
  output = SD.open(SD_filename, FILE_WRITE);                            // clear output.csv file on SD card reader
  if(!output){
    Serial.println("Failed to open file for writing");
    return;
  }
  output.close();
  write_header();
}

void init_sensors()
{

  mpu.begin(); 
     
//    if(!mpu.begin()){
//     Serial.println("MPU Failed");
//     return;
//    }

    mpu.setAccelerometerRange(MPU6050_RANGE_4_G);
    mpu.setGyroRange(MPU6050_RANGE_500_DEG);
    mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

    bme.begin();  
    if(!bme.begin()){
     Serial.println("BME Failed");
     return;
    }

    bme.setSampling(Adafruit_BME280::MODE_NORMAL,
                    Adafruit_BME280::SAMPLING_X4,   // temperature
                    Adafruit_BME280::SAMPLING_NONE, // pressure off
                    Adafruit_BME280::SAMPLING_X4,   // humidity
                    Adafruit_BME280::FILTER_OFF,
                    Adafruit_BME280::STANDBY_MS_0_5 );

  ens160.begin();
    if(!ens160.begin()){
     Serial.println("ens160 Failed");
     return;
    }
  ens160.setMode(ENS160_OPMODE_STD);

    
}

#define WINDOW_SIZE 20  // Define the size of the window
#define BUFFER_SIZE 60

float calculateMovingAverage(float dataArray[], int dataSize) {
  float sum = 0;
  for (int i = 0; i < dataSize; i++) {
    sum += dataArray[i];
  }

  return sum / dataSize;
}

int count=0;
float averagedFrequencyReadings[BUFFER_SIZE]; // Array to store averaged frequency readings
int peakCount = 0;
int peakCount_low = 0;
double maxPeakValue = 0.0;
double peakValues[10], peakValues_low[10];
bool notend, notend_low = true; 
double globalmax, globalmin;
unsigned long lastcurpeaks = 0; // Define lastPeakTime as a global variable
unsigned long lastcurpeaks_low = 0; // Define lastPeakTime as a global variable
bool isHoldingBreath = false; // Initialize breath-holding state
int lastValue = 0;         // Previous reading
int filteredValue = 0;         // Previous reading
std::vector<int> fifoBuffer;
double lastfreq = 0;         // Previous reading
double lastdiff=0;

unsigned long lastPeakTime = 0; // Variable to store the time of the last peak
unsigned long lastPeakTime2 = 0; // Variable to store the time of the last peak

unsigned long cooldownPeriod = 1500; // Cooldown period in milliseconds (adjust as needed)

void read_data()
{

    executionCounter++;
    unsigned long currentTime = millis();

    if (currentTime - lastExecutionTime >= 1000) {
        executionCounter = 0;
        lastExecutionTime = currentTime;
    }
//
//    BME_T = bme.readTemperature();
//    BME_RH = bme.readHumidity();
//    sensors_event_t a, g, temp;
//    mpu.getEvent(&a, &g, &temp);
//    ax_x = a.acceleration.x;
//    ax_y = a.acceleration.y;
//    ax_z = a.acceleration.z;
//    Serial.println(ax_z);
    unsigned long c = millis();

    if (c - lastSensorReadTime >= 3000) {
        lastSensorReadTime = c;
//        ens160.measure();
//        ENS_TVOC = ens160.getTVOC();
//        ENS_CO2 = ens160.geteCO2();
    }


    
  if (abs(-frequency_0 - lastValue) <= 15) {
//      filteredValue = 0.1 * (-frequency_0) + (1 - 0.1) * filteredValue;
      peakDetection.add(-frequency_0);                     // adds a new data point
      double filtered = peakDetection.getFilt();   // moving average
      
    
    currentIndex = (currentIndex + 1) % BUFFER_SIZE;
    int diff = (filtered - lastfreq) > 0 ? 1 : 0;
//    Serial.println(diff);
    lastfreq = filtered;
    averagedFrequencyReadings[currentIndex] = filtered;
    fifoBuffer.push_back(diff); 

    int lastcurrent = currentIndex- 10;
    if (lastcurrent < 0){
        lastcurrent = lastcurrent + BUFFER_SIZE; // Adjust for circular buffer
    }

  float *maxElement = std::max_element(averagedFrequencyReadings, averagedFrequencyReadings + BUFFER_SIZE);
  float *minElement = std::min_element(averagedFrequencyReadings, averagedFrequencyReadings + BUFFER_SIZE);
  float difference = *maxElement - *minElement;
  
    
  if (difference < 2) {


    if (!isHoldingBreath) {
//        Serial.println("Starting holding breath");
        isHoldingBreath = true;
//        char message[100]; 
//        snprintf(message, sizeof(message), "Start Holding Breath");
//        mqttClient.publish("iot", message);
        unsigned long currentMillis = millis(); // Get current time in milliseconds
        char message[100]; 
        if(control){
//          sprintf(message, "Start holding breath - Timestamp: %lu", currentMillis); // Format message with timestamp 
//          mqttClient.publish("iot", message); // Publish message with timestamp

        }
       
    } 
  } else {
      if (isHoldingBreath) {
//        Serial.println("Ending holding breath");
        isHoldingBreath = false;
        unsigned long currentMillis = millis(); // Get current time in milliseconds
        char message[100]; 
        if(control){
//          sprintf(message, "Stop holding breath - Timestamp: %lu", currentMillis); // Format message with timestamp 
//          mqttClient.publish("iot", message); // Publish message with timestamp
        }
      }
  }




        int sum1=0;
      int sum2=0;
      for(int i=0; i<5; i++){
        sum1 += fifoBuffer[i];
        sum2 += fifoBuffer[i+5];
      }

    //if( diff==1 && lastdiff==0){
    //  Serial.println("Peak");
    //}
    lastdiff= diff;
    //Serial.print("sum1: ");
    //Serial.print(sum1);
    //Serial.print(", sum2: ");
    //Serial.println(sum2);
        if (sum1 < 3 && sum2 > 2 && (millis() - lastPeakTime) > cooldownPeriod) {
//            Serial.println("Minima");
            lastPeakTime = millis(); // Update last peak time    
            if(!isHoldingBreath){
              char message[100]; 
              if(control){
                sprintf(message, "Minima - Timestamp: %lu", lastPeakTime); // Format message with timestamp
                mqttClient.publish("iot", message); // Publish message with timestamp
              }
              
            }
            
//            fifoBuffer.clear(); // Erase all elements from the vector
    //        char message[100]; 
    //        snprintf(message, sizeof(message), "Minima");
    //        mqttClient.publish("iot", "Minima");
        }
    
        if (sum2 < 3 && sum1 > 2 && (millis() - lastPeakTime2) > cooldownPeriod && (millis() - lastPeakTime) > 500) {
//            Serial.println("Maxima");
            if(!isHoldingBreath){
              char message[100]; 
              if(control){
                sprintf(message, "Maxima - Timestamp: %lu", lastPeakTime2); // Format message with timestamp
                mqttClient.publish("iot", message); // Publish message with timestamp
              }
              else if(monitor){
                float monitordiff = millis()-lastPeakTime2;
                float rr = 60000/monitordiff;
                char rrStr[10]; // Buffer to hold the float as a string
                dtostrf(rr, 6, 2, rrStr); // Convert float to string with 2 decimal places
                char message[50]; // Buffer to hold the final message
                sprintf(message, "da621,%s", rrStr); // Format message with timestamp
                
                Serial.println(message); // Print the message for debugging
                mqttClient.publish("iot", message); // Publish message with timestamp
              }
              lastPeakTime2 = millis(); // Update last peak time

             
  //            fifoBuffer.clear(); // Erase all elements from the vector
      //        mqttClient.publish("iot", "Maxima");
            }
        }
    
    if (fifoBuffer.size() > 10) {
        fifoBuffer.erase(fifoBuffer.begin());
    }
//Serial.println(difference);

// if ((difference > 8)) {
//         if(&averagedFrequencyReadings[lastcurrent] == maxElement) {
//             unsigned long curpeaks = millis();
//             unsigned long timeInterval = curpeaks - lastcurpeaks;
//             lastcurpeaks = currentTime; // Update last peak time for the next peak
            
//             // Calculate respiratory rate
//             double instantaneousRespiratoryRate = 1000.0 / timeInterval; 
//             double respiratoryRate = instantaneousRespiratoryRate * 60.0; 

//         //    Serial.print("Respiratory Rate (BPM): ");
//         //    Serial.println(respiratoryRate);

            
// //           Serial.println("peak");
//             if(notend){
//         //       Serial.println(filtered); 
// //              Serial.println("NEFES VER SEKERIM"); 

//             }
//             else{
// //            Serial.println(difference); 
//             float amp = filtered-globalmax+100; 
//             char message[100]; 
// //            Serial.println(amp); 

//             snprintf(message, sizeof(message), "Peak_amplitude: %.2f Respiratory_rate: %.2f", amp, respiratoryRate);
            
//         //      mqttClient.publish("iot", message); 

//         }
//         if (notend){
//             peakValues[peakCount] = filtered; 
//             peakCount++;
//             if (peakCount == 10) {
//                 globalmax = *std::max_element(peakValues+1, peakValues + 10);
//                 notend = false;
//             }

//         }
//         }
//         else if (&averagedFrequencyReadings[lastcurrent] == minElement){

//             unsigned long curpeaks_low = millis();
//             unsigned long timeInterval_low = curpeaks_low - lastcurpeaks_low;
//             lastcurpeaks_low = currentTime; // Update last peak time for the next peak
            

            
// //           Serial.println("nefes aldi");
        

//             if(notend_low){
//         //       Serial.println(filtered); 
// //              Serial.println("NEFES AL SEKERIM"); 

//             }
//             else{
//         //      Serial.println(difference); 
//             float amp_low = filtered-globalmin+100; 
//             char message[100]; 
// //            Serial.println(amp_low); 
  
//             // snprintf(message, sizeof(message), "Peak_amplitude: %.2f Respiratory_rate: %.2f", amp, respiratoryRate);
            
//         //      mqttClient.publish("iot", message); 

//         }
//         if (notend_low){
//             peakValues_low[peakCount_low] = filtered; 
//             peakCount_low++;
//             if (peakCount_low == 10) {
//                 globalmin = *std::min_element(peakValues_low+1, peakValues_low + 10);
//                 notend_low = false;
//             }

//         }     
//         }
    
    
//   } 
    }

  lastValue = -frequency_0;



    
//    currentIndex = (currentIndex + 1) % WINDOW_SIZE;
//    frequencyReadings[currentIndex] = calculateMovingAverage(frequencyReadings, WINDOW_SIZE);
//
//    int lastcurrent = currentIndex - 2;
//    if (lastcurrent < 0){
//        lastcurrent = lastcurrent + WINDOW_SIZE; // Adjust for circular buffer
//    }
//
//    int beforeCount = 0;
//    int afterCount = 0;
//
//    // Iterate over the values before lastcurrent
//    for (int i = lastcurrent - 4; i < lastcurrent; ++i) {
//        // Handle circular buffer indexing
//        int index = (i + WINDOW_SIZE) % WINDOW_SIZE;
//
//        // Check if the value is greater than the value at lastcurrent
//        if (frequencyReadings[index] > frequencyReadings[lastcurrent]) {
//            beforeCount++;
//        }
//    }
//
//    // Iterate over the values after lastcurrent
//    for (int i = lastcurrent + 1; i <= lastcurrent + 4; ++i) {
//        // Handle circular buffer indexing
//        int index = (i + WINDOW_SIZE) % WINDOW_SIZE;
//
//        // Check if the value is greater than the value at lastcurrent
//        if (frequencyReadings[index] > frequencyReadings[lastcurrent]) {
//            afterCount++;
//        }
//    }
//
//    if (beforeCount >= 4 && afterCount >= 4) {
//        // Your condition is satisfied
//        Serial.println("Peak detected!");
//    }
//
//    Serial.printf("%.2f\n", frequencyReadings[currentIndex]); // Print the current moving average

    // You can perform further actions based on the peak detection
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

void write_data()
{
      // Note: esp_time is the time at the end of the read_PCNT
      output = SD.open(SD_filename,FILE_APPEND); 
      read_data();
      output.printf("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f \n", esp_time, frequency_0, frequency_1, BME_T, BME_RH, ax_x, ax_y, ax_z);
      //output.printf("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f \n", esp_time, frequency_0, frequency_1, BME_T, BME_RH, ax_x, ax_y, ax_z);
      output.close();
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
        write_data();
        
      }
    }
    
    delay(1); // units are millisecs
  }
    if (!mqttClient.connected()) {
      reconnect();
    }
    mqttClient.loop();
}


//--------SD card reader functions ---------------------------------------------------------------------

void writeFile(fs::FS &fs, const char * path, const char * message){
  Serial.printf("Writing file: %s\n", path);

  File file = fs.open(path, FILE_WRITE);
  if(!file){
    Serial.println("Failed to open file for writing");
    return;
  }
  if(file.print(message)){
    Serial.println("File written");
  } else {
    Serial.println("Write failed");
  }
  file.close();
}

void appendFile(fs::FS &fs, const char * path, const char * message){
  Serial.printf("Appending to file: %s\n", path);

  File file = fs.open(path, FILE_APPEND);
  if(!file){
    Serial.println("Failed to open file for appending");
    return;
  }
  if(file.print(message)){
    Serial.println("Message appended");
  } else {
    Serial.println("Append failed");
  }
  file.close();
}

void renameFile(fs::FS &fs, const char * path1, const char * path2){
  Serial.printf("Renaming file %s to %s\n", path1, path2);
  if (fs.rename(path1, path2)) {
    Serial.println("File renamed");
  } else {
    Serial.println("Rename failed");
  }
}