#include <ESP8266WiFi.h>
#include <time.h>

#define DO_LOGGING 1

const char * SSID = "*********";
const char * PASSWORD = "**********";

const int PORT = 4242;

WiFiServer server(PORT);

#define EN D5
#define DIR D6
#define PUL D7

// TODO: Maybe save state in memory so we don't have to resync in case of power loss? 
// Doesn't seem super necessary but could be interesting to figure out.
uint8 enabled = 0;
uint8 override_enabled = 0;
int32 override_time = 0;
int32 override_length = 0;

uint8 day_enabled[7] = { 0 };
int32 day_time[7] = { 0 };
int32 day_length[7] = { 0 };

uint8 manual_stop_received = 0;

#define TZ "EST+5EDT,M3.2.0/2,M11.1.0/2" 

void setup() {
    Serial.begin(115200);
    delay(100);

    configTime(TZ, "pool.ntp.org");

 #if DO_LOGGING
    Serial.print("\nConnecting to ");
    Serial.print(SSID);
#endif

    WiFi.mode(WIFI_STA);
    //WiFi.begin(SSID, PASSWORD);
    WiFi.begin();

    while (WiFi.status() != WL_CONNECTED) {
        delay(500);

#if DO_LOGGING
        Serial.print(".");
#endif

    }

#if DO_LOGGING
    Serial.print("\nWiFi connected -> ");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());

    Serial.print("\nSyncing time");
#endif

    while (time(nullptr) < 1000000) {
        delay(500);

#if DO_LOGGING
        Serial.print(".");
#endif

    }

#if DO_LOGGING
    Serial.print("\n");
#endif

    time_t tnow = time(nullptr);
    Serial.println(ctime(&tnow));

    server.begin();

    pinMode(EN, OUTPUT);
    pinMode(DIR, OUTPUT);
    pinMode(PUL, OUTPUT);

    // Turn clockwise (I think?)
    digitalWrite(DIR, LOW);

    // Disable motor
    digitalWrite(EN, LOW);
}

WiFiClient client;
bool client_connected = false;

void loop() {
    // Server Logic
    server_logic();
        
    // Curtain logic
    curtain_logic();
}

const int32 MINUTES_IN_A_DAY = 1440;
void curtain_logic() {
    if (enabled) {
        time_t tnow = time(nullptr);
        tm* time_struct = localtime(&tnow);
        int time_in_minutes = time_struct->tm_hour * 60 + time_struct->tm_min;

        // Figure out next time to open:
        int32 next_time_to_open = -1;
        int32 next_length_to_open = 0;

        if (override_enabled) {
            next_time_to_open = override_time;
            next_length_to_open = override_length;
        }
        else {
            int today_wday = (time_struct->tm_wday - 1) % 7; // Sunday = 0 from time.h, but I use Monday = 0
            if (time_in_minutes < day_time[today_wday] && day_enabled[today_wday]) {
                next_time_to_open = day_time[today_wday];
                next_length_to_open = day_length[today_wday];
            }
        }

        next_length_to_open = max(next_length_to_open, 1); // I have a minimum length of 1 minute to avoid having to deal with repeat opens

        if (next_time_to_open >= 0) {
            int32 mod_value = mod(next_time_to_open - time_in_minutes, MINUTES_IN_A_DAY);
            if (mod_value <= next_length_to_open && mod_value > 0) {
                if (!manual_stop_received) {
                    openingLoop(next_length_to_open);
                }
            }
        }
    }
    else {
        // To reset a manual stop, we need to cycle enable.
        manual_stop_received = 0;
    }
}

int32 mod(int32 a, int32 b) {
    int32 c = a % b;
    return (c < 0) ? c + b : c;
}

const int STEPS_PER_ROTATION = 800;
const float DISTANCE_PER_ROTATION = 0.078f;
const float CURTAIN_DISTANCE = 1.0f;
const int NUM_STEPS_TO_OPEN = (CURTAIN_DISTANCE * STEPS_PER_ROTATION) / DISTANCE_PER_ROTATION;
const int MIN_MILLIS_PER_STEP = 1;

bool opening = 0;

void openingLoop(int length) {

    opening = 1;

    int length_in_millis = length * 60 * 1000;
    int num_millis_per_step = max(length_in_millis / NUM_STEPS_TO_OPEN, MIN_MILLIS_PER_STEP);
    int last_step_millis = 0;

    float num_turns = NUM_STEPS_TO_OPEN / STEPS_PER_ROTATION;

    int num_millis_per_turn = max((int)(length_in_millis / num_turns), MIN_MILLIS_PER_STEP);

#if DO_LOGGING
    Serial.println("Opening now");
    Serial.println(length_in_millis);
    Serial.println(num_millis_per_step);
#endif


    if (num_millis_per_step > MIN_MILLIS_PER_STEP) {
        for (int i = 0; i < NUM_STEPS_TO_OPEN;) {
            int current_millis = millis();

            server_logic(); // Maintain Wifi communication

            if (!enabled || manual_stop_received) {
                break;
            }

            // TODO, we could have better math here to better handle hitches, but that's also not really necessary.
            if (current_millis - last_step_millis >= num_millis_per_turn) {
                last_step_millis = millis();
                rotate_full_turn();
                i += STEPS_PER_ROTATION;
            }

            // TODO: maybe delay here, depending on how fast we're running
            // delay(1)
        }
    }
    else { // If we're doing instant open, let's forget about comms
        digitalWrite(EN, HIGH);
        for (int i = 0; i < NUM_STEPS_TO_OPEN; i++) {
            step();
            delay(num_millis_per_step);
        }
        digitalWrite(EN, LOW);
    }

    

#if DO_LOGGING
    Serial.println("Finished opening");
#endif

    opening = 0;

}

void rotate_full_turn() {
    digitalWrite(EN, HIGH);
    for (int i = 0; i < STEPS_PER_ROTATION; i++) {
        step();
    }
    digitalWrite(EN, LOW);
}

void step() 
{
    digitalWrite(PUL, LOW);
    delay(1);
    digitalWrite(PUL, HIGH);
    delay(1);
}

void server_logic() {
    if (client_connected && client.connected()) {
        handleClient();
    }
    else {
        if (client_connected) {
            client.stop();
        }

        client = server.available();
        client.setNoDelay(true);

        if (!client) {
            client_connected = false;
        }
        else {

#if DO_LOGGING
            Serial.print("Connection address: ");
            Serial.println(client.remoteIP().toString().c_str());
#endif

            client_connected = true;

            handleClient();
        }
    }
}

int32 swap_int32(int32 val)
{
    val = ((val << 8) & 0xFF00FF00) | ((val >> 8) & 0xFF00FF);
    return (val << 16) | ((val >> 16) & 0xFFFF);
}

int32 get_le_int(uint8 * p) {
    int32 result;
    memcpy(&result, p, 4);
    return swap_int32(result);
}

void handleClient() {
    if (client.connected() && client.available()) {
        int8 packet_length = client.read();

        // Wait for the whole packet to be recieved
        while (client.available() < packet_length - 1);
        uint8 * buffer = (uint8*)malloc(packet_length - 1);
        client.readBytes(buffer, packet_length - 1);

        uint8 version = buffer[0];
        uint8 command = buffer[1];

        uint8 data_length = packet_length - 3;

        if (data_length) {
            uint8 * data = buffer + 2;

#if DO_LOGGING
            Serial.print("Received packet: Length = ");
            Serial.print(packet_length);
            Serial.print(" Version = ");
            Serial.print(version);
            Serial.print(" Command = ");
            Serial.print(command);

            if (command < 40) {
                Serial.print(" Data = ");

                if (data_length == 1) {
                    Serial.println(data[0]);
                }
                else {
                    Serial.println(get_le_int(&data[0]));
                }
            }
            else {
                if (data_length == 2) {
                    Serial.print(" Data = ");
                    Serial.print(data[0]);
                    Serial.print(", ");
                    Serial.println(data[1]);
                }
                else {
                    Serial.print(" Data = ");
                    Serial.print(data[0]);
                    Serial.print(", ");
                    Serial.println(get_le_int(&data[1]));
                }
            }
#endif

            /////////

            if (command == 20) {
                enabled = data[0];
            }
            else if (command == 30) {
                override_enabled = data[0];
            }
            else if (command == 31) {
                override_time = get_le_int(&data[0]);
            }
            else if (command == 32) {
                override_length = get_le_int(&data[0]);
            }
            else if (command == 40) {
                uint8 index = data[0];
                day_enabled[index] = data[1];
            }
            else if (command == 41) {
                uint8 index = data[0];
                day_time[index] = get_le_int(&data[1]);
            }
            else if (command == 42) {
                uint8 index = data[0];
                day_length[index] = get_le_int(&data[1]);
            }
        }
        else {

#if DO_LOGGING
            Serial.print("Received packet: Length = ");
            Serial.print(packet_length);
            Serial.print(" Version = ");
            Serial.print(version);
            Serial.print(" Command = ");
            Serial.println(command);
#endif

            if (command == 10 && !opening) {
                openingLoop(0);
            }
            else if (command == 11) {
                manual_stop_received = 1;
            }
        }

        free(buffer);

        if (command == 2 || command == 3) {
            int32 response_length = 76;
            uint8* response = (uint8*) malloc(response_length);
            int32 cursor = 0;

            memcpy(&response[cursor++], &response_length, 1);
            memcpy(&response[cursor++], &version, 1);
            memcpy(&response[cursor++], &command, 1);
            memcpy(&response[cursor++], &enabled, 1);
            memcpy(&response[cursor++], &override_enabled, 1);

            int32 _override_time = swap_int32(override_time);
            int32 _override_length = swap_int32(override_length);

            memcpy(&response[cursor], &_override_time, 4);
            cursor += 4;
            memcpy(&response[cursor], &_override_length, 4);
            cursor += 4;

            for (int i = 0; i < 7; i++) {
                memcpy(&response[cursor++], &day_enabled[i], 1);

                int32 day_time_i = swap_int32(day_time[i]);
                int32 day_length_i = swap_int32(day_length[i]);

                memcpy(&response[cursor], &day_time_i, 4);
                cursor += 4;
                memcpy(&response[cursor], &day_length_i, 4);
                cursor += 4;
            }

            client.write(response, response_length);
            client.flush();
        }
        else {
            uint8 response_length = 3;
            uint8* response = (uint8*) malloc(response_length);
            int32 cursor = 0;

            memcpy(&response[cursor++], &response_length, 1);
            memcpy(&response[cursor++], &version, 1);
            memcpy(&response[cursor++], &command, 1);

            client.write(response, response_length);
            client.flush();
        }
    }
}