
package com.ponysdk.sample.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.query.memory.FilteringTools;
import com.ponysdk.sample.client.datamodel.Pony;

public class PonyServiceImpl implements com.ponysdk.sample.service.pony.PonyService {

    private static Logger log = LoggerFactory.getLogger(PonyServiceImpl.class);

    private static AtomicLong id = new AtomicLong();

    private final ConcurrentHashMap<Long, Pony> ponyByID = new ConcurrentHashMap<Long, Pony>();

    public PonyServiceImpl() {
        final Random rdm = new Random();
        for (int i = 0; i < 10; i++) {
            addPony(new Pony(id.incrementAndGet(), "Altai horseBengin", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "American Warmblood", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Falabella", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Friesian horse", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Mustang", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Altai horse", rdm.nextInt(10), "Equus ferus caballus"));
        }

    }

    @Override
    public Result<List<Pony>> findPonys(final Query query) throws Exception {
        log.info("Looking for pony with criterion #" + query.getCriteria());
        final List<Pony> datas = new ArrayList<Pony>(ponyByID.values());
        return FilteringTools.select(query, datas);
    }

    @Override
    public Pony createPony(final Pony pony) throws Exception {
        pony.setId(id.incrementAndGet());
        addPony(pony);
        return pony;
    }

    @Override
    public Void deletePony(final Long id) throws Exception {
        ponyByID.remove(id);
        return null;
    }

    @Override
    public Result<List<Pony>> findPonyChilds(final Long fatherID) throws Exception {
        final Pony father = ponyByID.get(fatherID);
        final List<Pony> subPonyList = new ArrayList<Pony>();
        subPonyList.add(new Pony(id.incrementAndGet(), father.getName() + " child 1", 1, father.getRace()));
        subPonyList.add(new Pony(id.incrementAndGet(), father.getName() + " child 2", 2, father.getRace()));
        subPonyList.add(new Pony(id.incrementAndGet(), father.getName() + " child 3", 3, father.getRace()));
        return new Result<List<Pony>>(subPonyList);
    }

    private void addPony(final Pony pony) {
        ponyByID.put(pony.getId(), pony);
    }

}
