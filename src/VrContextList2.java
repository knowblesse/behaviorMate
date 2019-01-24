import processing.core.PApplet;
import java.util.Iterator;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.ArrayList;

public class VrContextList2 extends BasicContextList {
    protected float previous_location;
    protected UdpClient[] comms;
    protected JSONObject position_json;
    protected JSONObject position_data;
    protected JSONObject log_json;
    protected String[] comm_ids;
    protected JSONObject context_info;
    protected String startString;
    protected String stopString;

    public VrContextList2(JSONObject context_info,
            float track_length) throws Exception {
        super(context_info, track_length, null);

        this.comm_ids = context_info.getJSONArray(
            "display_controllers").getStringArray();
        this.context_info = context_info;

        position_data = new JSONObject();
        position_json = new JSONObject();
        log_json = new JSONObject();
        log_json.setJSONObject("context", new JSONObject());

        this.previous_location = -1;

        JSONObject start_msg = new JSONObject();
        start_msg.setString("action", "start");
        this.startString = start_msg.toString();

        JSONObject stop_msg = new JSONObject();
        stop_msg.setString("action", "stop");
        this.stopString = stop_msg.toString();
    }

    public void setupVr(String vr_file) {
        JSONObject clear_msg = new JSONObject();
        clear_msg.setString("action", "clear");
        sendMessage(clear_msg.toString());

        JSONObject vr_config = parseJSONObject(
            BehaviorMate.parseJsonFile(vr_file).toString());
        JSONArray objects = vr_config.getJSONArray("objects");
        JSONObject msg_json = new JSONObject();
        for (int i=0; i < objects.size(); i++) {
            msg_json.setJSONObject("object", objects.getJSONObject(i));
            sendMessage(msg_json.toString());
        }

        Iterator<String> keys = vr_config.keyIterator();
        while (keys.hasNext()) {
            String key = keys.next();
            System.out.println(key);
            if (!key.equals("objects")) {
                msg_json = new JSONObject();
                do {
                    try {
                        msg_json.setInt(key, vr_config.getInt(key));
                        break;
                    } catch(RuntimeException e) { }
                    try {
                        msg_json.setFloat(key, vr_config.getFloat(key));
                        break;
                    } catch(RuntimeException e) { }
                    try {
                        msg_json.setBoolean(key, vr_config.getBoolean(key));
                        break;
                    } catch(RuntimeException e) { }
                    try {
                        msg_json.setString(key, vr_config.getString(key));
                        break;
                    } catch(RuntimeException e) { }
                } while(false);
                System.out.println(msg_json.toString());
                sendMessage(msg_json.toString());
            }
        }

        /*
        if (!vr_config.isNull("skybox")) {
            msg_json = new JSONObject();
            msg_json.setString("skybox", vr_config.getString("skybox"));
            sendMessage(msg_json.toString());
        }*/
    }

    public boolean setupComms(ArrayList<UdpClient> comms) {
        this.comms = new UdpClient[comm_ids.length];
        for (int i = 0; i < comm_ids.length; i++) {
            boolean found = false;
            for (UdpClient c : comms) {
                if (c.getId().equals(this.comm_ids[i])) {
                    found = true;
                    this.comms[i] = c;
                    JSONObject msg_json = new JSONObject();
                    msg_json.setJSONObject(
                        "view",
                        this.context_info.getJSONArray("views")
                                         .getJSONObject(i));
                    c.sendMessage(msg_json.toString());
                    c.sendMessage(stopString);
                    try {
                        Thread.sleep(10);
                    } catch (Exception e){}
                }
            }
            if (!found) {
                return false;
            }
        }

        JSONObject clear_msg = new JSONObject();
        clear_msg.setString("action", "clear");
        sendMessage(clear_msg.toString());
        return true;
    }

    public void setId(String id) {
        this.id = id;
    }

    /*public void setCues(JSONArray cues) {
        sendMessage(this.startString);

        JSONObject clearMessage = new JSONObject();
        clearMessage.setString("type", "cues");
        clearMessage.setString("action", "clear");
        sendMessage(clearMessage.toString());

        JSONObject createMessage = new JSONObject();
        createMessage.setString("type", "cues");
        createMessage.setString("action", "create");
        createMessage.setJSONArray("cues", cues);
        sendMessage(createMessage.toString());

        if (this.active == -1) {
            sendMessage(this.stopString);
        }

    }

    public void addCues(JSONArray cues, String list_name) {
        sendMessage(this.startString);

        JSONObject createMessage = new JSONObject();
        createMessage.setString("type", "cues");
        createMessage.setString("action", "create");
        createMessage.setString("list_name", list_name);
        createMessage.setJSONArray("cues", cues);
        sendMessage(createMessage.toString());

        if (this.active == -1) {
            sendMessage(this.stopString);
        }
    }

    public void clearCueList(String list_name) {
        if (this.active == -1) {
            sendMessage(this.startString);
        }

        JSONObject clearMessage = new JSONObject();
        clearMessage.setString("type", "cues");
        clearMessage.setString("action", "clear");
        clearMessage.setString("list_name", list_name);
        sendMessage(clearMessage.toString());

        if (this.active == -1) {
            sendMessage(this.stopString);
        }
    }*/

    public boolean check(float position, float time, int lap,
                         String[] msg_buffer) {
        boolean inZone = false;
        int i=0;
        for (; i < this.contexts.size(); i++) {
            if (this.contexts.get(i).check(position, time)) {
                inZone = true;
                break;
            }
        }

        if ((this.active != -1) && (position != previous_location)) {
            position_data.setFloat("y", position/10);
            position_json.setJSONObject(
                "position", position_data);

            sendMessage(position_json.toString());
            previous_location = position;
        }

        if ((!inZone) && (this.active != -1)) {
            this.active = -1;
            this.status = "off";
            sendMessage(this.stopString);

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = log_json.toString().replace("\n","");
        } else if((inZone) && (this.active != i)) {
            this.active = i;
            this.status = "on";
            setupVr(context_info.getString("vr_file"));
            //for (i = 0; i < 3; i++) {
                sendMessage(this.startString);
            //}
            position_data.setFloat("y", position/10);
            position_json.setJSONObject(
                "position", position_data);

            sendMessage(position_json.toString());
            previous_location = position;

            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "start");
            msg_buffer[0] = log_json.toString().replace("\n","");
        }


        return (this.active != -1);
    }

    public void suspend() {
        this.active = -1;
        this.status = "off";
        sendMessage(this.stopString);

            JSONObject clear_msg = new JSONObject();
            clear_msg.setString("action", "clear");
            sendMessage(clear_msg.toString());
    }

    public void stop(float time, String[] msg_buffer) {
        if (this.active != -1) {
            log_json.setFloat("time", time);
            log_json.getJSONObject("context").setString("action", "stop");
            msg_buffer[0] = log_json.toString().replace("\n","");
        }

        this.active = -1;
        this.status = "off";
        sendMessage(this.stopString);
    }

    protected void sendMessage(String message) {
        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
        }
    }



    protected void sendMessageSlow(String message) {
        for (int i=0; i < this.comms.length; i++) {
            this.comms[i].sendMessage(message);
            try {
                Thread.sleep(10);
            } catch(Exception e) {

            }
        }
    }
}
