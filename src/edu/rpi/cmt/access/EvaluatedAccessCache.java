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
package edu.rpi.cmt.access;

import edu.rpi.cmt.access.Access.AccessStatsEntry;
import edu.rpi.cmt.access.Acl.CurrentAccess;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/** This provides a cache of evaluated CurrentAccess objects. Rather than create
 * a composite key, which itself involves creating a new object per search, we
 * instead creae  hierarchical set of tables, each indexed by an element of the
 * key. These levels are: <ul>
 * <li>owner href: the owner of the entity</li>
 * <li>accessor href: who's trying to get access</li>
 * <li>desired access</li>
 * <li>max access: filters the allowed access</li>
 * <li>acl: a string representation of the acl</li>
 *
 * @author douglm
 *
 */
public class EvaluatedAccessCache implements Serializable {
  private transient static Logger log;

  private static Map<String, Map> ownerHrefs = new HashMap<String, Map>();

  /* Back end of the queue is the most recently referenced. */
  private static LinkedList<String> accessorQueue = new LinkedList<String>();

  private static AccessStatsEntry accessorQueueLen =
    new AccessStatsEntry("Access cache accessor queue len");

  private static AccessStatsEntry numGets =
    new AccessStatsEntry("Access cache gets");

  private static AccessStatsEntry numHits =
    new AccessStatsEntry("Access cache hits");

  private static AccessStatsEntry numAclTables =
    new AccessStatsEntry("Access cache ACL tables");

  private static AccessStatsEntry numEntries =
    new AccessStatsEntry("Access cache entries");

  private static Collection<AccessStatsEntry> stats = new ArrayList<AccessStatsEntry>();

  static {
    stats.add(accessorQueueLen);
    stats.add(numGets);
    stats.add(numHits);
    stats.add(numAclTables);
    stats.add(numEntries);
  }

  /**
   * @param ownerHref
   * @param accessorHref
   * @param desiredPriv
   * @param maxAccess
   * @param acl
   * @return CurrentAccess or null
   */
  public static CurrentAccess get(String ownerHref,
                                  String accessorHref,
                                  PrivilegeSet desiredPriv,
                                  PrivilegeSet maxAccess,
                                  String acl) {
    numGets.count++;
    Map<String, Map> accessors = ownerHrefs.get(ownerHref);

    if (accessors == null) {
      return null;
    }

    /* ===================== desired priv ================== */

    Map<PrivilegeSet, Map> desiredPrivs = accessors.get(accessorHref);

    if (desiredPrivs == null) {
      return null;
    }

    accessorQueue.remove(accessorHref);
    accessorQueue.add(accessorHref);

    /* ===================== max priv ================== */

    Map<PrivilegeSet, Map> maxPrivs = desiredPrivs.get(desiredPriv);

    if (maxPrivs == null) {
      return null;
    }

    /* ===================== acl ================== */

    Map<String, CurrentAccess> acls = maxPrivs.get(maxAccess);

    if (acls == null) {
      return null;
    }

    /* ==================== finally access =============== */

    CurrentAccess ca = acls.get(acl);

    if (ca != null) {
      numHits.count++;
    }

    return ca;
  }

  /**
   * @param ownerHref
   * @param accessorHref
   * @param desiredPriv
   * @param maxAccess
   * @param acl
   * @param ca
   */
  public static void put(String ownerHref,
                         String accessorHref,
                         PrivilegeSet desiredPriv,
                         PrivilegeSet maxAccess,
                         String acl,
                         CurrentAccess ca) {
    boolean found = true;

    Map<String, Map> accessors = ownerHrefs.get(ownerHref);

    if (accessors == null) {
      accessors = new HashMap<String, Map>();
      ownerHrefs.put(ownerHref, accessors);
      found = false;
    }

    accessorQueue.remove(accessorHref);
    accessorQueue.add(accessorHref);

    /* ===================== desired priv ================== */

    Map<PrivilegeSet, Map> desiredPrivs = null;
    if (found) {
      // Try a search
      desiredPrivs = accessors.get(accessorHref);
    }

    if (desiredPrivs == null) {
      desiredPrivs = new HashMap<PrivilegeSet, Map>();
      accessors.put(accessorHref, desiredPrivs);
      found = false;
    }

    /* ===================== max priv ================== */

    Map<PrivilegeSet, Map> maxPrivs = null;
    if (found) {
      // Try a search
      maxPrivs = desiredPrivs.get(desiredPriv);
    }

    if (maxPrivs == null) {
      maxPrivs = new HashMap<PrivilegeSet, Map>();
      desiredPrivs.put(desiredPriv, maxPrivs);
      found = false;
    }

    /* ===================== acl ================== */

    Map<String, CurrentAccess> acls = null;
    if (found) {
      // Try a search
      acls = maxPrivs.get(maxAccess);
    }

    if (acls == null) {
      acls = new HashMap<String, CurrentAccess>();
      maxPrivs.put(maxAccess, acls);
      numAclTables.count++;
      found = false;
    }

    /* ==================== finally store =============== */

    if (found) {
      // Let's see if it's the same - it ought to be

      CurrentAccess tca = acls.get(acl);
      if (tca != null) {
        if (!tca.equals(ca)) {
          // That's bad.
          error("Current access in table does not match, table:" + tca +
                " new version " + ca);
        }
      }
    }

    numEntries.count++;
    acls.put(acl, ca);
  }

  /** Get the cache statistics
   *
   * @return Collection of stats
   */
  public static Collection<AccessStatsEntry> getStatistics() {
    accessorQueueLen.count = accessorQueue.size();

    return stats;
  }

  private static Logger getLog() {
    if (log == null) {
      log = Logger.getLogger(EvaluatedAccessCache.class.getName());
    }

    return log;
  }

  private static void error(String msg) {
    getLog().error(msg);
  }
}