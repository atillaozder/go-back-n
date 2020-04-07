# Go-Back-N

## Description
Design a simple transport protocol that provides reliable datagram service by using one of the pipelining protocols. The protocol will be responsible for ensuring data is delivered in order, without duplicates, missing data, or errors. Write code that will transfer a file reliably from between two nodes (a sender and a receiver). Do NOT have to implement connection open/close etc. Assume that the receiver is run first and will wait indefinitely, and the sender can just send the data to the receiver.

## Requirements
- Transfer the file name reliably.
- Transfer the file contents reliably.
- The receiver must write the contents it receives into the local directory with the appropriate file name.
- The receiver must print out the received data to the file in order and without errors.
- The sender and receiver must print out specified debugging messages to standard error and a log file.
- Your sender and receiver must gracefully exit.
- Your code must be able to transfer a file with any number of packets dropped, damaged, duplicated, and
delayed, and under a variety of different available bandwidths and link latencies.

## Command Line
The client program takes command line argument of the remote IP address and port number, and the name of the file to transmit. The syntax for launching your sending program is therefore: 

sender -r "recv host":"recv port" -f "filename"
 
"recv host" (Required) The IP address of the remote host in a.b.c.d format.

"recv port" (Required) The UDP port of the remote host.

"filename" (Required) The name of the file (with its full path) to send. 
 
To aid in grading and debugging, your sending program should print out messages to the console: When a sender sends a packet (including retransmission), it should print the following to standard error:

"timestamp" [send data] start (length)
 
where timestamp is a timestamp (down to the microsecond), start is the beginning offset of the data sent in the packet, and length is the amount of the data sent in that packet. When your sender receives an acknowledgement, you should also print to standard error:

"timestamp" [recv ack] end

where end is the last offset that was acknowledged. You may also print some messages of your own to indicate timeouts, etc, depending on your design, but make it concise and readable. 

The command line syntax for your receiving program is given below.

receiver -p "recv port"

"recv port" (Required) The UDP port to listen on.

The program will start up and will bind to a local port—once bound, it will print out the following to standard error:

"timestamp" [bound] port

To aid in grading and debugging, your receiving program should print out messages to standard error:  When the receiver receives a valid data packet, it should print

"timestamp" [recv data] start (length) status

where start is the beginning offset of the data sent in the packet, and length is the amount of the data sent in that packet, and status is one of ACCEPTED (in-order), ACCEPTED (out-of-order), or IGNORED. If a corrupt packet arrives, it should print to standard error:

"timestamp" [recv corrupt packet]

Similar to sender, you may add your own output messages. Both the sender and the receiver should print out a message to standard error after completion of file transfer, and then exit:

"timestamp" [completed]
