package de.gsi.dataset;

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

    List<String> getErrorList();

    List<String> getInfoList();

    Map<String, String> getMetaInfo();

    List<String> getWarningList();

}
