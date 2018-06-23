
/*
 * Copyright (c) 2018 Sony Pictures Imageworks Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.imageworks.spcue.test.dao.oracle;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.imageworks.spcue.config.TestAppConfig;
import com.imageworks.spcue.DispatchHost;
import com.imageworks.spcue.Host;
import com.imageworks.spcue.HostDetail;
import com.imageworks.spcue.Source;
import com.imageworks.spcue.CueIce.HardwareState;
import com.imageworks.spcue.CueIce.HostTagType;
import com.imageworks.spcue.CueIce.LockState;
import com.imageworks.spcue.CueIce.ThreadMode;
import com.imageworks.spcue.RqdIce.HostReport;
import com.imageworks.spcue.RqdIce.RenderHost;
import com.imageworks.spcue.dao.AllocationDao;
import com.imageworks.spcue.dao.FacilityDao;
import com.imageworks.spcue.dao.HostDao;
import com.imageworks.spcue.dispatcher.Dispatcher;
import com.imageworks.spcue.service.HostManager;
import com.imageworks.spcue.util.CueUtil;

@Transactional
@ContextConfiguration(classes=TestAppConfig.class, loader=AnnotationConfigContextLoader.class)
@TransactionConfiguration(transactionManager="transactionManager")
public class HostDaoTests extends AbstractTransactionalJUnit4SpringContextTests  {

    private static final String TEST_HOST = "beta";

    @Resource
    protected AllocationDao allocationDao;

    @Resource
    protected HostDao hostDao;

    @Resource
    protected HostManager hostManager;

    @Resource
    protected FacilityDao facilityDao;

    public HostDaoTests() { }

    public static RenderHost buildRenderHost(String name) {

        RenderHost host = new RenderHost();
        host.name = name;
        host.bootTime = 1192369572;
        host.freeMcp = 7602;
        host.freeMem = 15290520;
        host.freeSwap = (int) CueUtil.MB512;
        host.load = 1;
        host.totalMcp = 19543;
        host.totalMem = (int) CueUtil.GB16;
        host.totalSwap = (int) CueUtil.GB2;
        host.nimbyEnabled = false;
        host.numProcs = 2;
        host.coresPerProc = 400;
        host.tags = new ArrayList<String>();
        host.tags.add("linux");
        host.tags.add("64bit");
        host.state = HardwareState.Up;
        host.facility = "spi";
        host.attributes = new HashMap<String,String>();
        host.attributes.put("freeGpu", String.format("%d", CueUtil.MB512));
        host.attributes.put("totalGpu", String.format("%d", CueUtil.MB512));

        return host;
    }

    @Test
    public void testInit() { }

    @BeforeTransaction
    public void clear() {
        jdbcTemplate.update(
            "DELETE FROM host WHERE str_name=?", TEST_HOST);
    }

    @AfterTransaction
    public void destroy() {
        jdbcTemplate.update(
            "DELETE FROM host WHERE str_name=?", TEST_HOST);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testInsertHost() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        assertEquals(Long.valueOf(CueUtil.GB16 - Dispatcher.MEM_RESERVED_SYSTEM), jdbcTemplate.queryForObject(
                "SELECT int_mem FROM host WHERE str_name=?",
                Long.class, TEST_HOST));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testInsertHostFQDN1() {
        String TEST_HOST_NEW = "ice-ns1.yvr";
        String FQDN_HOST = TEST_HOST_NEW + ".spimageworks.com";
        hostDao.insertRenderHost(buildRenderHost(FQDN_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail hostDetail = hostDao.findHostDetail(TEST_HOST_NEW);
        assertEquals(TEST_HOST_NEW, hostDetail.name);

        Host host = hostDao.findHost(FQDN_HOST);
        HostDetail hostDetail2 = hostDao.getHostDetail(host);
        assertEquals(TEST_HOST_NEW, hostDetail2.name);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testInsertHostFQDN2() {
        String TEST_HOST_NEW = "compile21";
        String FQDN_HOST = TEST_HOST_NEW + ".spimageworks.com";
        hostDao.insertRenderHost(buildRenderHost(FQDN_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail hostDetail = hostDao.findHostDetail(TEST_HOST_NEW);
        assertEquals(TEST_HOST_NEW, hostDetail.name);

        Host host = hostDao.findHost(FQDN_HOST);
        HostDetail hostDetail2 = hostDao.getHostDetail(host);
        assertEquals(TEST_HOST_NEW, hostDetail2.name);

    }

    @Test
    @Transactional
    @Rollback(true)
    public void testInsertHostAlternateOS() {

        RenderHost host = buildRenderHost(TEST_HOST);
        host.attributes.put("SP_OS", "spinux1");

        hostDao.insertRenderHost(host,
                hostManager.getDefaultAllocationDetail());

        assertEquals("spinux1",jdbcTemplate.queryForObject(
                "SELECT str_os FROM host_stat, host " +
                "WHERE host.pk_host = host_stat.pk_host " +
                "AND host.str_name=?",String.class, TEST_HOST), "spinux1");
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testInsertHostDesktop() {

        RenderHost host = buildRenderHost(TEST_HOST);
        hostDao.insertRenderHost(host,
                hostManager.getDefaultAllocationDetail());

        assertEquals(Long.valueOf(CueUtil.GB16 - Dispatcher.MEM_RESERVED_SYSTEM), jdbcTemplate.queryForObject(
                "SELECT int_mem FROM host WHERE str_name=?",
                Long.class, TEST_HOST));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testUpdateThreadMode() {

        RenderHost host = buildRenderHost(TEST_HOST);
        host.nimbyEnabled = true;
        hostDao.insertRenderHost(host,
                hostManager.getDefaultAllocationDetail());

        HostDetail d = hostDao.findHostDetail(TEST_HOST);
        hostDao.updateThreadMode(d, ThreadMode.Auto);

        assertEquals(Integer.valueOf(ThreadMode.Auto.value()), jdbcTemplate.queryForObject(
                "SELECT int_thread_mode FROM host WHERE pk_host=?",
                Integer.class, d.id));

        hostDao.updateThreadMode(d, ThreadMode.All);

        assertEquals(Integer.valueOf(ThreadMode.All.value()), jdbcTemplate.queryForObject(
                "SELECT int_thread_mode FROM host WHERE pk_host=?",
                Integer.class, d.id));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testGetHostDetail() {

        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail host = hostDao.findHostDetail(TEST_HOST);
        hostDao.getHostDetail(host);
        hostDao.getHostDetail(host.getHostId());
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testIsHostLocked() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail host = hostDao.findHostDetail(TEST_HOST);
        assertEquals(hostDao.isHostLocked(host),false);

        hostDao.updateHostLock(host, LockState.Locked, new Source("TEST"));
        assertEquals(hostDao.isHostLocked(host),true);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testIsKillMode() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail host = hostDao.findHostDetail(TEST_HOST);
        assertFalse(hostDao.isKillMode(host));

        jdbcTemplate.update(
                "UPDATE host_stat SET int_swap_free = ?, int_mem_free = ? WHERE pk_host = ?",
                CueUtil.MB256, CueUtil.MB256, host.getHostId());

        assertTrue(hostDao.isKillMode(host));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testIsHostUp() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        assertTrue(hostDao.isHostUp(hostDao.findHostDetail(TEST_HOST)));

        hostDao.updateHostState(hostDao.findHostDetail(TEST_HOST),
                HardwareState.Down);
        assertFalse(hostDao.isHostUp(hostDao.findHostDetail(TEST_HOST)));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testHostExists() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        assertEquals(hostDao.hostExists(TEST_HOST),true);
        assertEquals(hostDao.hostExists("frickjack"),false);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testDeleteHost() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail host = hostDao.findHostDetail(TEST_HOST);
        assertEquals(hostDao.hostExists(TEST_HOST),true);
        hostDao.deleteHost(host);
        assertEquals(hostDao.hostExists(TEST_HOST),false);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testUpdateHostRebootWhenIdle() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail host = hostDao.findHostDetail(TEST_HOST);
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                "SELECT b_reboot_idle FROM host WHERE pk_host=?",
                Integer.class, host.getHostId()));
        hostDao.updateHostRebootWhenIdle(host, true);
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT b_reboot_idle FROM host WHERE pk_host=?",
                Integer.class, host.getHostId()));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void updateHostStats() {

        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        DispatchHost dispatchHost = hostDao.findDispatchHost(TEST_HOST);
        hostDao.updateHostStats(dispatchHost,
                CueUtil.GB8,
                CueUtil.GB8,
                CueUtil.GB8,
                CueUtil.GB8,
                CueUtil.GB8,
                CueUtil.GB8,
                1,
                1,
                100,
                new Timestamp(1247526000 * 1000l),
                "spinux1");

        Map<String,Object> result = jdbcTemplate.queryForMap(
                "SELECT * FROM host_stat WHERE pk_host=?",
                dispatchHost.getHostId());

        assertEquals(CueUtil.GB8, ((BigDecimal)
                (result.get("int_mem_total"))).longValue());
        assertEquals(CueUtil.GB8, ((BigDecimal)
                (result.get("int_mem_free"))).longValue());
        assertEquals(CueUtil.GB8, ((BigDecimal)
                (result.get("int_swap_total"))).longValue());
        assertEquals(CueUtil.GB8, ((BigDecimal)
                (result.get("int_swap_free"))).longValue());
        assertEquals(CueUtil.GB8, ((BigDecimal)
                (result.get("int_mcp_total"))).longValue());
        assertEquals(CueUtil.GB8, ((BigDecimal)
                (result.get("int_mcp_free"))).longValue());
        assertEquals(100, ((BigDecimal)
                (result.get("int_load"))).intValue());
        assertEquals(new Timestamp(1247526000 * 1000l),
                (Timestamp) result.get("ts_booted"));

    }

    @Test
    @Transactional
    @Rollback(true)
    public void updateHostResources() {

        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        DispatchHost dispatchHost = hostDao.findDispatchHost(TEST_HOST);
        HostReport report = new HostReport();
        report.host = buildRenderHost(TEST_HOST);
        report.host.coresPerProc = 1200;
        report.host.numProcs = 2;
        report.host.totalMem = (int) CueUtil.GB32;
        hostDao.updateHostResources(dispatchHost, report);

        // Verify what the original values are
        assertEquals(800, dispatchHost.cores);
        assertEquals(800, dispatchHost.idleCores);
        assertEquals(CueUtil.GB16 - Dispatcher.MEM_RESERVED_SYSTEM,
                dispatchHost.idleMemory);
        assertEquals(CueUtil.GB16-  Dispatcher.MEM_RESERVED_SYSTEM,
                dispatchHost.memory);

        dispatchHost = hostDao.findDispatchHost(TEST_HOST);

        // Now verify they've changed.
        assertEquals(2400, dispatchHost.cores);
        assertEquals(2400, dispatchHost.idleCores);
        assertEquals(CueUtil.GB32 -  Dispatcher.MEM_RESERVED_SYSTEM,
                dispatchHost.idleMemory);
        assertEquals(CueUtil.GB32-  Dispatcher.MEM_RESERVED_SYSTEM,
                dispatchHost.memory);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testGetDispatchHost() {
        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail hostDetail = hostDao.findHostDetail(TEST_HOST);
        DispatchHost dispatchHost = hostDao.findDispatchHost(TEST_HOST);

        assertEquals(dispatchHost.name, TEST_HOST);
        assertEquals(dispatchHost.allocationId, hostDetail.getAllocationId());
        assertEquals(dispatchHost.id, hostDetail.getHostId());
        assertEquals(dispatchHost.cores, hostDetail.cores);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testUpdateHostSetAllocation() {

        hostDao.insertRenderHost(buildRenderHost(TEST_HOST),
                hostManager.getDefaultAllocationDetail());

        HostDetail hostDetail = hostDao.findHostDetail(TEST_HOST);

        hostDao.updateHostSetAllocation(hostDetail,
                hostManager.getDefaultAllocationDetail());

        hostDetail = hostDao.findHostDetail(TEST_HOST);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testUpdateHostSetManualTags() {
        DispatchHost host = hostManager.createHost(buildRenderHost(TEST_HOST));

        hostDao.tagHost(host,"frick", HostTagType.Manual);
        hostDao.tagHost(host,"jack", HostTagType.Manual);
        hostDao.recalcuateTags(host.id);

        String tag = jdbcTemplate.queryForObject(
                "SELECT str_tags FROM host WHERE pk_host=?",String.class, host.id);
        assertEquals("unassigned beta frick jack", tag);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testUpdateHostSetOS() {
        DispatchHost host = hostManager.createHost(buildRenderHost(TEST_HOST));
        hostDao.updateHostOs(host, "foo");
        String tag = jdbcTemplate.queryForObject(
                "SELECT str_os FROM host_stat WHERE pk_host=?",String.class, host.id);
        assertEquals("foo", tag);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testChangeTags() {
        DispatchHost host = hostManager.createHost(buildRenderHost(TEST_HOST));

        String tag = jdbcTemplate.queryForObject(
                "SELECT str_tags FROM host WHERE pk_host=?",String.class, host.id);
        assertEquals("unassigned beta", tag);

        hostDao.removeTag(host, "linux");
        hostDao.recalcuateTags(host.id);

        assertEquals("unassigned beta", jdbcTemplate.queryForObject(
                "SELECT str_tags FROM host WHERE pk_host=?",String.class, host.id));

        hostDao.tagHost(host, "32bit",HostTagType.Manual);
        hostDao.recalcuateTags(host.id);

        assertEquals("unassigned beta 32bit", jdbcTemplate.queryForObject(
                "SELECT str_tags FROM host WHERE pk_host=?",String.class, host.id));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testGetStrandedCoreUnits() {
        DispatchHost host = hostManager.createHost(buildRenderHost(TEST_HOST));

        jdbcTemplate.update(
                "UPDATE host SET int_mem_idle = ? WHERE pk_host = ?",
                CueUtil.GB, host.getHostId());

        assertEquals(host.idleCores, hostDao.getStrandedCoreUnits(host));

        jdbcTemplate.update(
                "UPDATE host SET int_mem_idle = ? WHERE pk_host = ?",
                CueUtil.GB2, host.getHostId());

        assertEquals(0, hostDao.getStrandedCoreUnits(host));

        // Check to see if fractional cores is rounded to the lowest
        // whole core properly.
        jdbcTemplate.update(
                "UPDATE host SET int_cores_idle=150, int_mem_idle = ? WHERE pk_host = ?",
                CueUtil.GB, host.getHostId());

        assertEquals(100, hostDao.getStrandedCoreUnits(host));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testIsPreferShow() {
        DispatchHost host = hostManager.createHost(buildRenderHost(TEST_HOST));
        assertFalse(hostDao.isPreferShow(host));
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testIsNimby() {
        DispatchHost host = hostManager.createHost(buildRenderHost(TEST_HOST));
        assertFalse(hostDao.isNimbyHost(host));
    }
}

