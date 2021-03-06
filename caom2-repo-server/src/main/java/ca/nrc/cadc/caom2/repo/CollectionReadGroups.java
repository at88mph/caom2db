/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2014.                            (c) 2014.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.caom2.repo;

import ca.nrc.cadc.ac.GroupURI;
import ca.nrc.cadc.util.PropertiesReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class CollectionReadGroups {

    private static final String COLLECTION_READ_GROUPS_FILE = "collection-read-groups.properties";
    private static final Logger log = Logger.getLogger(CollectionReadGroups.class);

    private String collection;
    private String config;

    public CollectionReadGroups(String collection) {
        this(collection, COLLECTION_READ_GROUPS_FILE);
    }

    public CollectionReadGroups(String collection, String config) {
        if (collection == null) {
            throw new IllegalArgumentException("the collection parameter is required.");
        }

        this.collection = collection;
        this.config = config;
    }

    public List<GroupURI> getReadGroups() throws IllegalArgumentException {
        PropertiesReader properties = new PropertiesReader(config);

        log.debug("Getting read groups for collection " + collection + " from config " + config);
        String readGroupsString = properties.getFirstPropertyValue(collection);
        if (readGroupsString == null) {
            throw new RuntimeException("no entry for " + collection + " in " + config);
        }

        List<String> readGroups = new ArrayList<String>();
        if (readGroupsString != null) {
            readGroups = Arrays.asList(readGroupsString.split(" "));
        }

        List<GroupURI> readGroupURIs = new ArrayList<GroupURI>();
        for (String readGroup: readGroups) {
            // Validates URI structure before returning the list
            readGroupURIs.add(new GroupURI(readGroup));
        }

        log.debug("Found " + readGroupURIs.size() + " groups for collection " + collection + ": "
            + Arrays.toString(readGroups.toArray()));

        return readGroupURIs;
    }

}
