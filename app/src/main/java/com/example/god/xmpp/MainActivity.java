package com.example.god.xmpp;

import android.app.ProgressDialog;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {

    public static final String HOST = "52.78.215.218";
    public static final int PORT = 5222;
    public static final String SERVICE = "oo";
    public static final String USERNAME = "ohs";
    public static final String PASSWORD = "zx2000";

    private XMPPConnection connection;
    private ArrayList<String> messages = new ArrayList<>();
    private Handler mHandler = new Handler();

    private EditText recipient;
    private EditText textMessage;
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recipient = (EditText) this.findViewById(R.id.toET);
        textMessage = (EditText) this.findViewById(R.id.chatET);
        listview = (ListView) this.findViewById(R.id.listMessages);
        setListAdapter();

        // Set a listener to send a chat text message
        Button send = (Button) this.findViewById(R.id.sendBtn);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String to = recipient.getText().toString();
                String text = textMessage.getText().toString();
                Message msg = new Message(to, Message.Type.chat);
                msg.setBody(text);
                if (connection != null) {
                    connection.sendPacket(msg);
                    messages.add(connection.getUser() + ":");
                    messages.add(text);
                    setListAdapter();
                }
            }
        });
        connect();
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
        if (connection != null) {
            // Add a packet listener to get messages sent to us
            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new PacketListener() {
                @Override
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String fromName = StringUtils.parseBareAddress(message.getFrom());

                        messages.add(fromName + ":");
                        messages.add(message.getBody());
                        // Add the incoming message to the list view
                        mHandler.post(new Runnable() {
                            public void run() {
                                setListAdapter();
                            }
                        });
                    }
                }
            }, filter);
        }
    }

    private void setListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listitem, messages);
        listview.setAdapter(adapter);
    }


    public void connect() {

        final ProgressDialog dialog = ProgressDialog.show(this, "Connecting...", "Please wait...", false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                // Create a connection
                ConnectionConfiguration connConfig = new ConnectionConfiguration(HOST, PORT, SERVICE);
                SASLAuthentication.supportSASLMechanism("MD5", 0);
                System.setProperty("smack.debugEnabled", "true");
                connConfig.setCompressionEnabled(false);
                connConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
                connConfig.setReconnectionAllowed(true);
                connConfig.setSendPresence(true);
                connConfig.setRosterLoadedAtLogin(true);
                XMPPConnection connection = new XMPPConnection(connConfig);
                try {
                    connection.connect();
                } catch (XMPPException ex) {
                    setConnection(null);
                }
                try {
                    connection.login(USERNAME, PASSWORD);
                    // Set the status to available
                    Presence presence = new Presence(Presence.Type.available);
                    connection.sendPacket(presence);
                    setConnection(connection);

                    Roster roster = connection.getRoster();
                    Collection<RosterEntry> entries = roster.getEntries();
                    for (RosterEntry entry : entries) {
                        Presence entryPresence = roster.getPresence(entry.getUser());
                        Presence.Type type = entryPresence.getType();
                    }
                } catch (XMPPException ex) {
                    setConnection(null);
                }
                dialog.dismiss();
            }
        });
        t.start();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            connection.disconnect();
        } catch (Exception e) {

        }
    }

}
