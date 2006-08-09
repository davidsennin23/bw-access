/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package edu.rpi.cmt.access.test;

import edu.rpi.cmt.access.AccessPrincipal;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/** Value object to represent a principal to allow testing of the access suite
 *
 *   @author Mike Douglass douglm@rpi.edu
 *  @version 1.0
 */
public abstract class Principal implements Comparator, AccessPrincipal {
  /** Account for the principal
   */
  private String account;  // null for guest

  /* groups of which this user is a member */
  protected Collection groups;

  // Derived from the groups.
  protected Collection groupNames;

  // For acl evaluation
  protected AccessPrincipal aclPrincipal;

  /* ====================================================================
   *                   Constructors
   * ==================================================================== */

  /** Create a guest principal
   */
  public Principal() {
    this(null);
  }

  /** Create a principal for an account
   *
   * @param  account            String account name
   */
  public Principal(String account) {
    this.account = account;
  }

  /* ====================================================================
   *                   Bean methods
   * ==================================================================== */

  /**
   * @return int kind
   */
  public abstract int getKind();

  /** Set the unauthenticated state.
   *
   * @param val
   */
  public void setUnauthenticated(boolean val) {
    if (val) {
      setAccount(null);
    }
  }

  /**
   * @return  boolean authenticated state
   */
  public boolean getUnauthenticated() {
    return getAccount() == null;
  }

  /**
   * @param val
   */
  public void setAccount(String val) {
    account = val;
  }

  /**
   * @return  String account name
   */
  public String getAccount() {
    return account;
  }

  /** Set of groups of which principal is a member
   *
   * @param val        Collection of Principal
   */
  public void setGroups(Collection val) {
    groupNames = null;
    groups = val;
  }

  /** Get the groups of which principal is a member.
   *
   * @return Collection    of Principal
   */
  public Collection getGroups() {
    if (groups == null) {
      groups = new TreeSet();
    }

    return groups;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @param val Principal
   */
  public void addGroup(Principal val) {
    getGroups().add(val);
  }

  /**
   * @return  Iterator over Principal
   */
  public Iterator iterateGroups() {
    return getGroups().iterator();
  }

  /**
   * @return boolean true for a guest principal
   */
  public boolean isUnauthenticated() {
    return account == null;
  }

  /** Set of groupNames of which principal is a member
   *
   * @param val        Set of String
   */
  public void setGroupNames(Collection val) {
    groupNames = val;
  }

  /** Get the group names of which principal is a member.
   *
   * @return Set    of String
   */
  public Collection getGroupNames() {
    if (groupNames == null) {
      groupNames = new TreeSet();
      Iterator it = iterateGroups();
      while (it.hasNext()) {
        Principal group = (Principal)it.next();
        groupNames.add(group.getAccount());
      }
    }
    return groupNames;
  }

  protected void toStringSegment(StringBuffer sb) {
    sb.append("account=");
    sb.append(account);
    sb.append(", kind=");
    sb.append(getKind());
  }

  /** Add a principal to the StringBuffer
   *
   * @param sb    StringBuffer for resultsb
   * @param name  tag
   * @param val   BwPrincipal
   */
  public static void toStringSegment(StringBuffer sb, String name, Principal val) {
    if (name != null) {
      sb.append(", ");
      sb.append(name);
      sb.append("=");
    }

    if (val == null) {
      sb.append("**NULL**");
    } else {
      sb.append("(");
      sb.append(val.getAccount());
      sb.append(")");
    }
  }

  /** Compare two strings. null is less than any non-null string.
   *
   * @param s1       first string.
   * @param s2       second string.
   * @return int     0 if the s1 is equal to s2;
   *                 <0 if s1 is lexicographically less than s2;
   *                 >0 if s1 is lexicographically greater than s2.
   */
  public static int compareStrings(String s1, String s2) {
    if (s1 == null) {
      if (s2 != null) {
        return -1;
      }

      return 0;
    }

    if (s2 == null) {
      return 1;
    }

    return s1.compareTo(s2);
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  public int compareTo(Object o) {
    return compare(this, o);
  }

  public int compare(Object o1, Object o2) {
    if (!(o1 instanceof Principal)) {
      return -1;
    }

    if (!(o2 instanceof Principal)) {
      return 1;
    }

    Principal p1 = (Principal)o1;
    Principal p2 = (Principal)o2;

    if (p1.getKind() < p2.getKind()) {
      return -1;
    }

    if (p1.getKind() > p2.getKind()) {
      return 1;
    }

    return compareStrings(p1.getAccount(), p2.getAccount());
  }

  public int hashCode() {
    int hc = 7 * (getKind() + 1);

    if (account != null) {
      hc = account.hashCode();
    }

    return hc;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("Principal{");

    toStringSegment(sb);
    sb.append("}");

    return sb.toString();
  }
}
