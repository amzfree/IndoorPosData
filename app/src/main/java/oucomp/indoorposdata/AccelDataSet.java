package oucomp.indoorposdata;

import android.util.JsonWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccelDataSet {
    private String type = "";
    private String name = "";
    private Date theTime = new Date();
    private List<AccelData> datalist = new ArrayList();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTheTime() {
        return theTime;
    }

    public void setTheTime(Date theTime) {
        this.theTime = theTime;
    }

    public int size() {
        return datalist.size();
    }

    public List<AccelData> getDatalist() {
        return datalist;
    }

    public void setDatalist(List<AccelData> datalist) {
        this.datalist = datalist;
    }

    public void add(AccelData data) {
        this.datalist.add(data);
    }

    public void writeJsonStream(OutputStream out) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginObject();
        writer.name("type").value(type);
        writer.name("name").value(name);
        writer.name("time").value(theTime.toString());
        writer.name("timeInMilli").value(theTime.getTime());
        writer.name("datalist");
        writeDataArray(writer, datalist);
        writer.endObject();
        writer.close();
    }

    public void writeDataArray(JsonWriter writer, List<AccelData> datalist) throws IOException {
        writer.beginArray();
        for (AccelData data : datalist) {
            data.writeJsonStream(writer);
        }
        writer.endArray();
    }
}
