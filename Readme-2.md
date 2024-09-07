# Key-Value Server
This project implements a server program functioning as a key-value store, allowing clients to store, retrieve, and delete key-value pairs over TCP and UDP protocols.

## Usage

### 1. Compile the program: (Optional  as already compiled)
To compile the program, navigate to the src folder and execute:
```bash
javac server/*.java client/*.java publicInterface/*.java

```
### 2. Run the server:
To start the server, run the ServerApp class using the following command-line arguments:


```bash
java server.ServerApp <host > <port> <port> <port> <port> <port> 

```
eg : java server.RMIServerStarter  "localhost" 12345 12346 12347 12348 12349

### 3. Run the client:
Use a separate terminal, execute the ClientApp class with the  command-line arguments:
```bash
java client.RMIClient<server-address> <port>
eg : java client.RMIClient "localhost" 12347
```
place <host-name> with the server's host name, <port-number> with the desired port number, and <protocol> with either TCP or UDP.

## Command Format
The server supports the following commands:

### 1. PUT
Format: PUT key value

Example: PUT something good

Returns : The key  if success , else NULL


### 2. GET
Format: GET value

Example: GET something

Returns : The corresponding value if found , else NULL


### 3. DELETE
Format: DELETE key

Example: DELETE something

Returns : The deleted key if delete success, else NULL
