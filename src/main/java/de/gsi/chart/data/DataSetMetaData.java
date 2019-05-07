package de.gsi.chart.data;

import java.util.List;
import java.util.Map;

/**
 * Interface for defining common measurement meta data tags
 *
 * @author rstein
 */
public interface DataSetMetaData {
    String TAG_OVERSHOOT = "over-range";
    String TAG_UNDERSHOOT = "under-range";
    String TAG_GAIN_RANGE = "gain-range";

    Map<String, String> getMetaInfo();

    List<String> getInfoList();

    List<String> getWarningList();

    List<String> getErrorList();

}
