
package com.ponysdk.sample.server;

import com.ponysdk.core.server.context.UIContext;
import com.ponysdk.core.server.context.UIContexts;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.sample.client.datamodel.PonyStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class PonyServiceImpl {

    private static Logger log = LoggerFactory.getLogger(PonyServiceImpl.class);

    private static AtomicLong id = new AtomicLong();

    private final ConcurrentHashMap<Long, Pony> ponyByID = new ConcurrentHashMap<>();
    private final List<PonyStock> stocks = new ArrayList<>();

    public PonyServiceImpl() {
        final Random rdm = new Random();
        for (int i = 0; i < 11; i++) {
            addPony(new Pony(id.incrementAndGet(), "Altai horseBengin", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "American Warmblood", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Falabella", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Friesian horse", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Mustang", rdm.nextInt(10), "Equus ferus caballus"));
            addPony(new Pony(id.incrementAndGet(), "Altai horse", rdm.nextInt(10), "Equus ferus caballus"));
        }

        initAndPushStock();
    }

    private void initAndPushStock() {
        for (int i = 0; i < 6; i++) {
            stocks.add(new PonyStock(id.incrementAndGet(), "Altai horseBengin v" + i, 5.5f, 100));
            stocks.add(new PonyStock(id.incrementAndGet(), "American Warmblood v" + i, 10f, 100));
            stocks.add(new PonyStock(id.incrementAndGet(), "Falabella v" + i, 15f, 100));
            stocks.add(new PonyStock(id.incrementAndGet(), "Friesian horse v" + i, 20f, 100));
            stocks.add(new PonyStock(id.incrementAndGet(), "Mustang v" + i, 30f, 100));
            stocks.add(new PonyStock(id.incrementAndGet(), "Altai horse v" + i, 50f, 100));
        }

        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final Random rdm = new Random();
            while (true) {
                try {
                    final int index = rdm.nextInt(stocks.size());
                    final float newPrice = rdm.nextFloat() * 100;
                    final PonyStock stock = stocks.get(index);
                    stock.setPrice(newPrice);
                    pushData(stock);
                    Thread.sleep(1000);
                } catch (final Exception e) {
                    log.error("", e);
                }
            }
        });
    }

    protected void pushData(final PonyStock stock) {
        Collection<UIContext> contexts = UIContexts.getContexts();
        for (final UIContext context : contexts) {
            context.pushToClient(stock);
        }
    }
    //
    // @Override
    // public Result<List<Pony>> findPonys(final Query query) throws Exception {
    // log.info("Looking for pony with criterion #" + query.getCriteria());
    // final List<Pony> datas = new ArrayList<Pony>(ponyByID.values());
    // return FilteringTools.select(query, datas);
    // }

    // @Override
    // public Pony createPony(final Pony pony) throws Exception {
    // pony.setId(id.incrementAndGet());
    // addPony(pony);
    // return pony;
    // }

    // @Override
    // public Void deletePony(final Long id) throws Exception {
    // ponyByID.remove(id);
    // return null;
    // }
    //
    // @Override
    // public Result<List<Pony>> findPonyChilds(final Long fatherID) throws Exception {
    // final Pony father = ponyByID.get(fatherID);
    // final List<Pony> subPonyList = new ArrayList<Pony>();
    // subPonyList.add(new Pony(id.incrementAndGet(), father.getName() + " child 1", 1, father.getRace()));
    // subPonyList.add(new Pony(id.incrementAndGet(), father.getName() + " child 2", 2, father.getRace()));
    // subPonyList.add(new Pony(id.incrementAndGet(), father.getName() + " child 3", 3, father.getRace()));
    // return new Result<List<Pony>>(subPonyList);
    // }

    private void addPony(final Pony pony) {
        ponyByID.put(pony.getId(), pony);
    }

}
