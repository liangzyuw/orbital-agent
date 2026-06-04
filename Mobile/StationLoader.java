// reads stations_formatted.txt and returns StationData
// line 1 = station name
// line 2 = orbital values

package Mobile;

import java.io.*;

public class StationLoader {

    public static StationData loadStation(
        String filename,
        String stationName
    ) throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String line;

        while ((line = reader.readLine()) != null) {

            String name = line.trim();

            String dataLine = reader.readLine();

            if (dataLine == null)
                break;

            if (name.equalsIgnoreCase(stationName)) {

                String[] values = dataLine.trim().split("\\s+");

                // position (r), velocity (v), Mean Anomaly Timestamps (t), and Mean Motion (n) values
                double t = Double.parseDouble(values[0]);
                double n = Double.parseDouble(values[1]);

                double[] r = {
                    Double.parseDouble(values[2]),
                    Double.parseDouble(values[3]),
                    Double.parseDouble(values[4])
                };

                double[] v = {
                    Double.parseDouble(values[5]),
                    Double.parseDouble(values[6]),
                    Double.parseDouble(values[7])
                };

                StationData station = new StationData(name, t, n, r, v);

                reader.close();

                return station;
            }
        }

        // System.out.println("Error: Could not find station with name: " + stationName);

        reader.close();

        return null;
    }
}