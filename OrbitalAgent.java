import Mobile.*;
import java.rmi.*;

public class OrbitalAgent extends Agent {
    
    public String[] destination;
    public int hopCount = 0;
    private int scenarioIndex = 0;

    private long startTime;
    private long endTime;
    private long lastHopTime;

    // spacecraft state of the agent
    private double[] myR;
    private double[] myV;

    private double[][] M_f;
    private double[][] N_f;
    private double[][] S_f;
    private double[][] T_f;
    private double[][] N_inv_f;

    private double t_f;
    private double s;
    private double c;
    private double n;

    // frame vectors
    private double[] x_hat;
    private double[] y_hat;
    private double[] z_hat;

    private double[] ref_r;
    private double[] ref_v;

    private String resultHistory = "";

    public OrbitalAgent() {
        destination = new String[0];
    }

    public OrbitalAgent(String[] args) {
        destination = args;
    }

    public OrbitalAgent(double[] r, double[] v, String[] destinations) {
        this.myR = r;
        this.myV = v;
        this.destination = destinations;
    }

    private void finishExecution() {
        endTime = System.nanoTime();

        double seconds = (endTime - startTime) / 1e12;
        // double seconds = (endTime - startTime) / 1e9;

        System.out.println("OrbitalAgent(" + getId() + ") completed all scenarios.");
        System.out.println("Orbital result history:");
        System.out.println(resultHistory);

        System.out.printf("Execution time: %.3f s\n", seconds);
    }

    public void init() {
        System.out.println("OrbitalAgent(" + getId() + ") starting orbital calculation");
        startTime = System.nanoTime();
        lastHopTime = startTime; 
        StationData station = fetchStationData();
        this.myR = station.r;
        this.myV = station.v;

        if (destination.length > 0) {
            String next = destination[hopCount];
            hopCount++;
            hop(next, "step");
        } else {
            finishExecution();
        }
    }

    public void setState(double[] r, double[] v) {
        this.myR = r;
        this.myV = v;
    }
    
    private StationData fetchStationData() {
        System.out.println("currentPlace = " + currentPlace);
        try {
            PlaceInterface place = (PlaceInterface) Naming.lookup(currentPlace);
            return place.getStationData();
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initStateMatrix(double t, double n) {
        this.n = n;
        this.t_f = t;

        this.c = Math.cos(n * t);
        this.s = Math.sin(n * t);

        this.M_f = initM(t);
        this.N_f = initN(t);
        this.N_inv_f = initNinv(t);
        this.S_f = initS(t);
        this.T_f = initT(t);
    }


    private double[][] initM(double t) {
        double[][] M = new double[3][3];
        M[0][0] = 4.0 - (3 * c);
        M[0][1] = 0;
        M[0][2] = 0;
        M[1][0] = 6 * (s - (n * t));
        M[1][1] = 1; 
        M[1][2] = 0;
        M[2][0] = 0; 
        M[2][1] = 0;
        M[2][2] = c;
        return M;
    }

    private double[][] initN(double t) {
        double[][] N = new double[3][3];
        N[0][0] = s / n;
        N[0][1] = (2 * (1 - c)) / n;
        N[0][2] = 0;
        N[1][0] = - (2 * (1 - c)) / n;
        N[1][1] = ((4 * s) - (n * t)) / n;
        N[1][2] = 0;
        N[2][0] = 0;
        N[2][1] = 0;
        N[2][2] = s / n;
        return N;
    }

    private double[][] initNinv(double t) {
        double[][] Ninv = new double[3][3];
        // for convenience, apply determinant to each term
        double det = n / ((8 * s * s * s) - (3 * s * s * n * t));
        Ninv[0][0] = ((4 * s * s) - (3 * s * n * t)) / det;
        Ninv[0][1] = (-2 * s * (1 - c)) / det;
        Ninv[0][2] = 0;
        Ninv[1][0] = ((2 * s * (1 - c))) / det;
        Ninv[1][1] = (s * s) / det;
        Ninv[1][2] = 0;
        Ninv[2][0] = 0;
        Ninv[2][1] = 0;
        // term mostly cancels determinant
        Ninv[2][2] = n / s;
        return Ninv;
    }

    private double[][] initS(double t) {
        double[][] S = new double[3][3];
        S[0][0] = 3 * n * s;
        S[0][1] = 0; 
        S[0][2] = 0;
        S[1][0] = (-6 * n * (1 - c));
        S[1][1] = 0;
        S[1][2] = 0;
        S[2][0] = 0;
        S[2][1] = 0;
        S[2][2] = n * s;
        return S;
    } 

    private double[][] initT(double t) {
        double[][] T = new double[3][3];
        T[0][0] = c;
        T[0][1] = 2 * s;
        T[0][2] = 0;
        T[1][0] = -2 * s;
        T[1][1] = (4 * c) - 3;
        T[1][2] = 0;
        T[2][0] = 0;
        T[2][1] = 0; 
        T[2][2] = c;
        return T;
    }

    private double norm(double[] v) {
        double s = 0;

        for(int i=0;i<3;i++)
            s += v[i] * v[i];

        return Math.sqrt(s);
    }

    private double[] unit(double[] v) {
        double n = norm(v);

        return new double[] {
            v[0] / n,
            v[1] / n,
            v[2] / n
        };
    }

    private double[] cross(double[] a, double[] b) {
        return new double[] {
            a[1]*b[2] - a[2]*b[1],
            a[2]*b[0] - a[0]*b[2],
            a[0]*b[1] - a[1]*b[0]
        };
    }

    private double[] scale(double s, double[] v) {
        return new double[] {
            s*v[0],
            s*v[1],
            s*v[2]
        };
    }

    private void buildFrame(double[] r, double[] v) {
        ref_r = r;
        ref_v = v;

        x_hat = unit(r);
        y_hat = unit(v);
        z_hat = cross(x_hat, y_hat);
    }

    private double[][] getBasisMatrix() {

        double[][] basis = new double[3][3];

        for(int i=0;i<3;i++) {
            basis[0][i] = x_hat[i];
            basis[1][i] = y_hat[i];
            basis[2][i] = z_hat[i];
        }

        return basis;
    }

    private double[][] getReverseBasisMatrix() {

        double[][] basis = new double[3][3];

        for(int i=0;i<3;i++) {
            basis[i][0] = x_hat[i];
            basis[i][1] = y_hat[i];
            basis[i][2] = z_hat[i];
        }

        return basis;
    }

    private double[] transformToFrame(double[] pos, double[] vel, double nMean) {

        double[] dr = vectorSub(pos, ref_r);

        double[] dv = vectorSub(vel, ref_v);

        double[] rot = cross(scale(nMean, z_hat), dr);

        dv = vectorSub(dv, rot);

        double[][] basis = getBasisMatrix();

        double[] drf = matrixTransform(basis, dr);
        double[] dvf = matrixTransform(basis, dv);

        double[] state = new double[6];

        for(int i=0;i<3;i++) {
            state[i] = drf[i];
            state[i+3] = dvf[i];
        }

        return state;
    }

    private double[] transformImpulseFromFrame(double[] dv) {

        double[][] basis = getReverseBasisMatrix();

        return matrixTransform(basis, dv);
    }

    // Multiply two matrices.
    private double[][] matrixMultiply(double[][] o1, double[][] o2) {
        double[][] prod = new double[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                prod[i][j] = 0;

                for (int k = 0; k < 3; k++) {
                    prod[i][j] += o1[i][k] * o2[k][j];
                }
            }
        }

        return prod;
    }
    
    private double[][] elementwiseOpMatrix(int op, double[][] o1, double[][] o2) {
        double[][] result = new double[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                result[i][j] = (op < 0) ? o1[i][j] - o2[i][j] : o1[i][j] + o2[i][j];
            }
        }
        return result;
    }

    private double[][] matrixAdd(double[][] o1, double[][] o2) {
        return elementwiseOpMatrix(1, o1, o2);
    } 

    private double[][] matrixSub(double[][] o1, double[][] o2) {
        return elementwiseOpMatrix(-1, o1, o2);
    }

    private double[] elementwiseOpVector(int op, double[] v1, double[] v2) {
        double[] result = new double[3];
        for(int i = 0; i < 3; i++) {
            result[i] = (op < 1) ? v1[i] - v2[i] : v1[i] + v2[i];
        }
        return result;
    }

    private double[] vectorAdd(double[] v1, double[] v2) {
        return elementwiseOpVector(1, v1, v2);
    }

    private double[] vectorSub(double[] v1, double[] v2) {
        return elementwiseOpVector(-1, v1, v2);
    }

    // Apply (multiply) a matrix A to a vector x, i.e. Ax = y.
    private double[] matrixTransform(double[][] A, double[] x) {
        double[] result = new double[3];
        for(int i = 0; i < 3; i++) {
            result[i] = 0;
            for(int j = 0; j < 3; j++) {
                result[i] += A[i][j] * x[j];
            }
        }
        return result;
    }

    private double[] invertVector(double[] v) {
        double[] r = new double[3];
        for(int i = 0; i < 3; i++) {
            r[i] = -(v[i]);
        }
        return r;
    }

    public double[] initialImpulse(double[] dr0, double[] dv0) {
        double[][] m = new double[3][3];
        m = matrixMultiply(N_inv_f, M_f);
        double[] v = new double[3];
        v = matrixTransform(m, dr0);
        v = invertVector(v);
        v = vectorSub(v, dv0);
        return v;
    }

    public double[] endImpulse(double[] dr0) {
        double[][] m = new double[3][3];
        m = matrixMultiply(N_inv_f, M_f);
        m = matrixMultiply(T_f, m);
        m = matrixSub(m, S_f);
        double[] v = new double[3];
        v = matrixTransform(m, dr0);
        return v;
    }

    private String formatVector(double[] v) {
        return String.format("%.2f, %.2f, %.2f", v[0], v[1], v[2]);
    }

    // step continues to the next destination if one exists
    public void step() {
        System.out.println("OrbitalAgent(" + getId() + ") running calculation after migration:");
        runOrbitalCalculation();

        if (hopCount < destination.length) {
            String next = destination[hopCount];
            hopCount++;
            System.out.println("Hop took: " + (Math.abs((System.nanoTime() - lastHopTime)/1e6)) + " ns");
            lastHopTime = System.nanoTime();
            hop(next, "step");
        } else {
            finishExecution();
        }
    }
    
    private void runOrbitalCalculation() {
        int scenario = scenarioIndex;

        StationData station = fetchStationData();
        if (station == null) {
            System.out.println("No station data found");
            return;
        }

        double t = station.t;
        double n = station.n;

        buildFrame(station.r, station.v);
        // transform the agent's spacecraft state
        double[] transformed = transformToFrame(myR, myV, station.n);

        double[] dr0 = new double[3];
        double[] dv0 = new double[3];

        for(int i=0;i<3;i++) {
            dr0[i] = transformed[i];
            dv0[i] = transformed[i+3];
        }

        initStateMatrix(t, n);

        System.out.println("Scenario " + scenario);
        System.out.printf("t = %.3f, n = %.3f\n", t, n);
        System.out.printf("dr0 = [%.2f, %.2f, %.2f]\n", dr0[0], dr0[1], dr0[2]);
        System.out.printf("dv0 = [%.2f, %.2f, %.2f]\n", dv0[0], dv0[1], dv0[2]);

        System.out.print("initial: \t");
        double[] deltaVi = initialImpulse(dr0, dv0);
        deltaVi = transformImpulseFromFrame(deltaVi);
        printVector(deltaVi);

        System.out.print("final: \t\t");
        double[] deltaVf = endImpulse(dr0);
        deltaVf = transformImpulseFromFrame(deltaVf);
        printVector(deltaVf);

        resultHistory += "Scenario " + scenario +
            " | t=" + String.format("%.3f", t) +
            ", n=" + String.format("%.3f", n) +
            " | initial=(" + formatVector(deltaVi) + ")" +
            " | final=(" + formatVector(deltaVf) + ")\n";

        scenarioIndex++;
    }

    private void printVector(double[] v) {
        System.out.printf("x: %.2f, y: %.2f, z: %.2f\n", v[0], v[1], v[2]);
    }
}