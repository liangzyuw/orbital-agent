import Mobile.*;

// Place 1: short transfer time
// Place 2: medium transfer time
// Place 3: longer transfer time
// Place 4: different mean motion / orbit rate

public class OrbitalAgent extends Agent {
    
    public String[] destination;
    public int hopCount = 0;

    private double[][] M_f;
    private double[][] N_f;
    private double[][] S_f;
    private double[][] T_f;
    private double[][] N_inv_f;

    private double t_f;
    private double s;
    private double c;
    private double n;

    private String resultHistory = "";

    // arrays for test cases
    private double[] tValues = {
        Math.PI / 4,
        Math.PI / 2,
        3 * Math.PI / 4,
        5 * Math.PI / 4
    };

    private double[] nValues = {
        1.0,
        1.0,
        0.8,
        1.2
    };

    private double[][] drValues = {
        {1, 1, 1},
        {2, 1, 1},
        {1, 2, 0.5},
        {3, -1, 1}
    };

    private double[][] dvValues = {
        {0, 0, 1},
        {0.2, 0, 1},
        {0, -0.1, 0.8},
        {0.1, 0.1, 1.2}
    };

    public OrbitalAgent() {
        destination = new String[0];
    }

    public OrbitalAgent(String[] args) {
        destination = args;
    }

    public void init() {
        System.out.println("OrbitalAgent(" + getId() + ") starting orbital calculation.");
        runOrbitalCalculation();

        if (destination.length > 0) {
            String next = destination[hopCount];
            hopCount++;
            hop(next, "step");
        } else {
            System.out.println("Orbital result history:");
            System.out.println(resultHistory);
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

    /*
     * Multiply two matrices.
     * @param o1 first (left) matrix
     * @param o2 second (right) matrix
     * @return 3x3 matrix result
     */
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
    
    /*
     * Add or subtract two 3x3 matrices.
     * @param op Subtract if op < 0, add otherwise
     * @param o1 first (left) matrix
     * @param o2 second (right) matrix
     * @return 3x3 matrix result
     */
    private double[][] elementwiseOpMatrix(int op, double[][] o1, double[][] o2) {
        double[][] result = new double[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                result[i][j] = (op < 0) ? o1[i][j] - o2[i][j] : o1[i][j] + o2[i][j];
            }
        }
        return result;
    }

    /*
     * Add two 3x3 matrices.
     */
    private double[][] matrixAdd(double[][] o1, double[][] o2) {
        return elementwiseOpMatrix(1, o1, o2);
    } 

    /*
     * Subtract two 3x3 matrices.
     */
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




    /*
     * Apply (multiply) a matrix A to a vector x, i.e. Ax = y.
     * @param A 3x3 matrix
     * @param x a 3-element vector
     * @return the 3-element vector result
     */
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
        System.out.println("OrbitalAgent(" + getId() + ") running calculation after migration.");
        runOrbitalCalculation();

        if (hopCount < destination.length) {
            String next = destination[hopCount];
            hopCount++;
            hop(next, "step");
        } else {
            System.out.println("OrbitalAgent(" + getId() + ") completed all scenarios.");
            System.out.println("Orbital result history:");
            System.out.println(resultHistory);
        }
    }

    private void runOrbitalCalculation() {
        int scenario = hopCount;

        if (scenario >= tValues.length) {
            scenario = tValues.length - 1;
        }

        double t = tValues[scenario];
        double n = nValues[scenario];

        double[] dr0 = drValues[scenario];
        double[] dv0 = dvValues[scenario];

        initStateMatrix(t, n);

        System.out.println("Scenario " + scenario);
        System.out.printf("t = %.3f, n = %.3f\n", t, n);
        System.out.printf("dr0 = [%.2f, %.2f, %.2f]\n", dr0[0], dr0[1], dr0[2]);
        System.out.printf("dv0 = [%.2f, %.2f, %.2f]\n", dv0[0], dv0[1], dv0[2]);

        System.out.print("initial: \t");
        double[] deltaVi = initialImpulse(dr0, dv0);
        printVector(deltaVi);

        System.out.print("final: \t\t");
        double[] deltaVf = endImpulse(dr0);
        printVector(deltaVf);

        resultHistory += "Scenario " + scenario +
            " | t=" + String.format("%.3f", t) +
            ", n=" + String.format("%.3f", n) +
            " | initial=(" + formatVector(deltaVi) + ")" +
            " | final=(" + formatVector(deltaVf) + ")\n";
    }

    private void printVector(double[] v) {
        System.out.printf("x: %.2f, y: %.2f, z: %.2f\n", v[0], v[1], v[2]);
    }
}