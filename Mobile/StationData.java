// container for station data
package Mobile;

import java.io.Serializable;

public class StationData implements Serializable {

    public String name;

    public double t;
    public double n;

    public double[] r;
    public double[] v;

    // orbital elements
    public double a;
    public double e;
    public double i;
    public double O;
    public double o;
    public double f;
    public double M;

    public StationData(String name, double t, double n, double[] r, double[] v) {
        this.name = name;
        this.t = t;
        this.n = n;
        this.r = r;
        this.v = v;
    }
}