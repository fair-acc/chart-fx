package de.gsi.microservice.rbac;

import java.util.Locale;
import java.util.Set;

/**
 * basic definition of common Role-Based-Access-Control (RBAC) roles
 *
 * original RBAC concept:
 * <ul>
 *  <li> Ferraiolo, D.F. & Kuhn, D.R. (October 1992). "Role-Based Access Control". 15th National Computer Security Conference: 554–563.
 *      https://csrc.nist.gov/CSRC/media/Publications/conference-paper/1992/10/13/role-based-access-controls/documents/ferraiolo-kuhn-92.pdf
 *  </li>
 *  <li> Sandhu, R., Coyne, E.J., Feinstein, H.L. and Youman, C.E. (August 1996). "Role-Based Access Control Models". IEEE Computer. 29 (2): 38–47. CiteSeerX 10.1.1.50.7649. doi:10.1109/2.485845
 *      https://csrc.nist.gov/projects/role-based-access-control
 *  </li>
 * </ul>
 */
public enum BasicRbacRole implements RbacRole<BasicRbacRole> {
    ADMIN(0), // highest priority in queues
    READ_WRITE(100),
    READ_ONLY(200),
    ANYONE(300),
    NULL(300); // lowest priority in queues

    private final int priority;

    BasicRbacRole(final int priority) {
        this.priority = priority;
    }

    @Override
    public String getName() {
        return this.toString();
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public BasicRbacRole getRole(final String roleName) {
        return valueOf(roleName.toUpperCase(Locale.UK));
    }

    public static RbacRole<?> getRoleStatic(final String roleString) {
        return NULL.getRole(roleString);
    }

    public static Set<RbacRole<?>> getRolesStatic(final String roleString) {
        return NULL.getRoles(roleString);
    }

    public static String getRolesStatic(final Set<RbacRole<?>> roleSet) {
        return NULL.getRoles(roleSet);
    }
}