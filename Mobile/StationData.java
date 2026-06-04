// container for station data
package Mobile;

import java.io.Serializable;

public class StationData implements Serializable {

    public String name;

    public double t;
    public double n;

    public double[] r;
    public double[] v;

    public StationData(String name, double t, double n, double[] r, double[] v) {
        this.name = name;
        this.t = t;
        this.n = n;
        this.r = r;
        this.v = v;
    }
}