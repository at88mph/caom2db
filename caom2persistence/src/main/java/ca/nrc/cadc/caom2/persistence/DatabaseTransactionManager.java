/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2011.                            (c) 2011.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.caom2.persistence;

import java.util.Deque;
import java.util.LinkedList;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Simple class that wraps a DataSource and performs basic transaction
 * operations.
 *
 * @author pdowler
 */
public class DatabaseTransactionManager implements TransactionManager {

    private static final Logger log = Logger.getLogger(DatabaseTransactionManager.class);

    private DataSourceTransactionManager writeTxnManager;
    private final TransactionDefinition defaultTxnDef = new DefaultTransactionDefinition();
    private final TransactionDefinition nested = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);
    private final Deque<Txn> transactions = new LinkedList();

    // transaction wrapper; the doCommit flag is here in case we want to add support
    // for TransactionDefinition.PROPAGATION_REQUIRED instead of NESTED and then skip 
    // the commit; it is not currently always true
    private class Txn {
        boolean doCommit;
        TransactionStatus status;
        
        Txn(TransactionStatus status, boolean doCommit) {
            this.status = status;
            this.doCommit = doCommit;
        }
    }
    
    private DatabaseTransactionManager() {
    }

    public DatabaseTransactionManager(DataSource ds) {
        this.writeTxnManager = new DataSourceTransactionManager(ds);
    }

    @Override
    public boolean isOpen() {
        return (!transactions.isEmpty());
    }

    @Override
    public void startTransaction() {
        TransactionDefinition defn;
        boolean doCommit = true;
        if (transactions.isEmpty()) {
            log.debug("startTransaction: default");
            defn = defaultTxnDef;
        } else if (writeTxnManager.isNestedTransactionAllowed()) {
            log.debug("startTransaction: nested");
            defn = nested;
        } else {
            throw new RuntimeException("nested transactions not supported by current configuration");
        }
        Txn txn = new Txn(writeTxnManager.getTransaction(defn), doCommit);
        transactions.push(txn);
        log.debug("startTransaction: " + transactions.size());
    }

    @Override
    public void commitTransaction() {
        if (transactions.isEmpty()) {
            throw new IllegalStateException("no transaction in progress");
        }
        Txn txn = transactions.pop();
        if (txn.doCommit) {
            log.debug("commitTransaction");
            writeTxnManager.commit(txn.status);
            log.debug("commitTransaction: OK");
        } else { 
            log.debug("commitTransaction - skip");
        }
        log.debug("commitTransaction: " + transactions.size());
    }

    @Override
    public void rollbackTransaction() {
        if (transactions.isEmpty()) {
            throw new IllegalStateException("no transaction in progress");
        }
        
        Txn txn = transactions.pop();
        if (txn.doCommit) {
            log.debug("rollbackTransaction");
            writeTxnManager.rollback(txn.status);
            log.debug("rollbackTransaction: OK");
        } else {
            log.debug("rollbackTransaction - skip");
        }
        log.debug("rollbackTransaction: " + transactions.size());
    }
}
