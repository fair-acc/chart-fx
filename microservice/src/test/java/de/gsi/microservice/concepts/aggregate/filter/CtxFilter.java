package de.gsi.microservice.concepts.aggregate.filter;

import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import de.gsi.microservice.concepts.aggregate.Filter;

public class CtxFilter implements Filter {
    public static final String WILD_CARD = "ALL";
    public static final String SELECTOR_PREFIX = "FAIR.SELECTOR.";
    /** selector string, e.g.: 'FAIR.SELECTOR.C=0:S=1:P=3:T=101' */
    public String selector;
    /** Beam-Production-Chain (BPC) ID - uninitialised/wildcard value = -1 */
    public int cid;
    /** Sequence ID -- N.B. this is the timing sequence number not the disruptor sequence ID */
    public int sid;
    /** Beam-Process ID (PID) - uninitialised/wildcard value = -1 */
    public int pid;
    /** timing group ID - uninitialised/wildcard value = -1 */
    public int gid;
    /** Beam-Production-Chain-Time-Stamp - UTC in [us] since 1.1.1980 */
    public long bpcts;
    /** stores the settings-supply relatex ctx name */
    public String ctxName;
    protected int hashCode = 0; // cached hash code

    public CtxFilter() {
        clear(); // NOPMD -- called during initialisation
    }

    @Override
    public void clear() {
        hashCode = 0;
        selector = null;
        cid = -1;
        sid = -1;
        pid = -1;
        gid = -1;
        bpcts = -1;
        ctxName = null;
    }

    @Override
    public void copyTo(final Filter other) {
        if (!(other instanceof CtxFilter)) {
            return;
        }
        final CtxFilter otherCtx = (CtxFilter) other;
        otherCtx.selector = this.selector;
        otherCtx.cid = this.cid;
        otherCtx.sid = this.sid;
        otherCtx.pid = this.pid;
        otherCtx.gid = this.gid;
        otherCtx.bpcts = this.bpcts;
        otherCtx.ctxName = this.ctxName;
        otherCtx.hashCode = this.hashCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CtxFilter otherCtx = (CtxFilter) o;
        if (hashCode != otherCtx.hashCode() || cid != otherCtx.cid || sid != otherCtx.sid || pid != otherCtx.pid || gid != otherCtx.gid || bpcts != otherCtx.bpcts) {
            return false;
        }

        return Objects.equals(selector, otherCtx.selector);
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        hashCode = selector != null ? selector.hashCode() : 0;
        hashCode = 31 * hashCode + cid;
        hashCode = 31 * hashCode + sid;
        hashCode = 31 * hashCode + pid;
        hashCode = 31 * hashCode + gid;
        hashCode = 31 * hashCode + Long.hashCode(bpcts);
        return hashCode;
    }

    public Predicate<CtxFilter> matches(final CtxFilter other) {
        return t -> this.equals(other);
    }

    public void setSelector(final String selector, final long bpcts) {
        try {
            assert selector != null : "selector string must not be null";
            assert bpcts > 0 : "BPCTS time stamp <= 0 :" + bpcts;
            clear();
            this.selector = selector;
            this.bpcts = bpcts;

            final String selectorUpper = selector.toUpperCase();
            if (selector.isBlank() || WILD_CARD.equals(selectorUpper)) {
                return;
            }

            final String[] identifiers = StringUtils.replace(selectorUpper, SELECTOR_PREFIX, "", 1).split(":");
            if (identifiers.length == 1 && WILD_CARD.equals(identifiers[0])) {
                return;
            }

            for (String tag : identifiers) {
                final String[] splitSubComponent = tag.split("=");
                assert splitSubComponent.length == 2 : "invalid selector: " + selector;
                final int value = splitSubComponent[1].equals(WILD_CARD) ? -1 : Integer.parseInt(splitSubComponent[1]);
                switch (splitSubComponent[0]) {
                case "C":
                    this.cid = value;
                    break;
                case "S":
                    this.sid = value;
                    break;
                case "P":
                    this.pid = value;
                    break;
                case "T":
                    this.gid = value;
                    break;
                default:
                    clear();
                    throw new IllegalArgumentException("cannot parse selector: '" + selector + "' sub-tag: " + tag);
                }
            }
        } catch (Throwable t) {
            clear();
            throw new IllegalArgumentException("cannot parse selector: '" + selector + "'", t);
        }
    }

    public String toString() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return '[' + CtxFilter.class.getSimpleName() + ": bpcts=" + bpcts + " (\"" + sdf.format(bpcts / 1000) + "\"), selector='" + selector + "', cid=" + cid + ", sid=" + sid + ", pid=" + pid + ", gid=" + gid + ']';
    }

    public static Predicate<CtxFilter> matches(final int cid, final int sid, final int pid, final long bpcts) {
        return t -> t.bpcts == bpcts && t.cid == cid && t.sid == sid && t.pid == pid;
    }

    public static Predicate<CtxFilter> matches(final int cid, final int sid, final long bpcts) {
        return t -> t.bpcts == bpcts && t.cid == cid && t.sid == sid;
    }

    public static Predicate<CtxFilter> matches(final int cid, final long bpcts) {
        return t -> t.bpcts == bpcts && t.cid == cid;
    }

    public static Predicate<CtxFilter> matches(final int cid, final int sid, final int pid) {
        return t -> t.cid == cid && t.sid == sid && t.pid == pid;
    }

    public static Predicate<CtxFilter> matches(final int cid, final int sid) {
        return t -> t.cid == cid && t.sid == sid;
    }

    public static Predicate<CtxFilter> matchesBpcts(final long bpcts) {
        return t -> t.bpcts == bpcts;
    }

    public static Predicate<CtxFilter> isOlderBpcts(final long bpcts) {
        return t -> t.bpcts < bpcts;
    }

    public static Predicate<CtxFilter> isNewerBpcts(final long bpcts) {
        return t -> t.bpcts > bpcts;
    }
}
