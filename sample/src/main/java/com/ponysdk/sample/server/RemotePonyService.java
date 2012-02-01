
package com.ponysdk.sample.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.query.memory.FilteringTools;
import com.ponysdk.sample.client.datamodel.Pony;

public class RemotePonyService implements com.ponysdk.sample.service.pony.PonyService {

    private static AtomicLong id = new AtomicLong();

    private final ConcurrentHashMap<Long, Pony> ponyByID = new ConcurrentHashMap<Long, Pony>();

    public RemotePonyService() {
        Random rdm = new Random();
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
        List<Pony> datas = new ArrayList<Pony>(ponyByID.values());
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
        final List<Pony> subPonyList = new ArrayList<Pony>();
        subPonyList.add(new Pony(id.incrementAndGet(), "SubPony 1", 7, "Equus ferus caballus"));
        subPonyList.add(new Pony(id.incrementAndGet(), "SubPony 2", 8, "Equus ferus caballus"));
        subPonyList.add(new Pony(id.incrementAndGet(), "SubPony 3", 9, "Equus ferus caballus"));
        return new Result<List<Pony>>(subPonyList);
    }

    private void addPony(final Pony pony) {
        ponyByID.put(pony.getId(), pony);
    }

}
