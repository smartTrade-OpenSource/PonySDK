/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.sample.client.page.addon;

import java.io.IOException;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;

public class SelectizeAddon extends PAddOnComposite<PElement> implements PTerminalEvent.Handler {

    IndexSearcher isearcher = null;

    String selectedSide;

    enum Type {
        CLIENT,
        CLASS,
        SECURITY,
        TENOR,
        SIDE
    }

    public SelectizeAddon() {
        super(Element.newInput());
        setTerminalHandler(this);

        //
        final Analyzer analyzer = new StandardAnalyzer();
        final Directory directory = new RAMDirectory();

        final IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer;
        try {
            writer = new IndexWriter(directory, config);
            final Document doc = new Document();
            final String text = "Test de ouf";

            final FieldType fieldType = new FieldType();
            fieldType.setIndexOptions(IndexOptions.NONE);
            fieldType.setStored(true);
            fieldType.setTokenized(false);
            doc.add(new Field("id", "12", fieldType));
            doc.add(new Field("fieldname", text, TextField.TYPE_STORED));

            writer.addDocument(doc);

            addAssetsType(writer);
            addTenor(writer);
            addClients(writer);
            addSide(writer);

            writer.close();
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            // Now search the index:
            final DirectoryReader ireader = DirectoryReader.open(directory);
            isearcher = new IndexSearcher(ireader);
            // Parse a simple query that searches for "text":
            // final QueryParser parser = new QueryParser("fieldname",
            // analyzer);
            // parser.setFuzzyMinSim(2f);

            final Term term = new Term("fieldname", "indesfed");
            final Query query = new FuzzyQuery(term);
            // final TopDocs hits = isearcher.search(query, 1000).scoreDocs;

            // final Query query = parser.parse("indeed");
            final ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
            // Iterate through the results:
            for (final ScoreDoc hit : hits) {
                System.err.println("Score : " + hit.score);
                final Document hitDoc = isearcher.doc(hit.doc);
                System.err.println("Found document" + hitDoc.getField("fieldname").stringValue());
            }
            // ireader.close();
            // directory.close();
        } catch (final Exception exception) {
            exception.printStackTrace();
        }

        // <input type="text" id="input-tags3" class="demo-default"
        // value="science,biology,chemistry,physics">
    }

    private void addSide(final IndexWriter writer) throws IOException {
        final Document doc1 = new Document();
        final FieldType fieldType1 = new FieldType();
        fieldType1.setIndexOptions(IndexOptions.NONE);
        fieldType1.setStored(true);
        fieldType1.setTokenized(false);
        doc1.add(new Field("id", "sell", fieldType1));
        doc1.add(new Field("fieldname", "Sell", TextField.TYPE_STORED));
        doc1.add(new Field("fieldname", "S", TextField.TYPE_STORED));
        doc1.add(new Field("type", Type.SIDE.name(), TextField.TYPE_STORED));
        doc1.add(new Field("desc", "side", TextField.TYPE_STORED));
        writer.addDocument(doc1);

        final Document doc2 = new Document();
        final FieldType fieldType2 = new FieldType();
        fieldType2.setIndexOptions(IndexOptions.NONE);
        fieldType2.setStored(true);
        fieldType2.setTokenized(false);
        doc2.add(new Field("id", "buy", fieldType2));
        doc2.add(new Field("fieldname", "Buy", TextField.TYPE_STORED));
        doc2.add(new Field("fieldname", "B", TextField.TYPE_STORED));
        doc2.add(new Field("type", Type.SIDE.name(), TextField.TYPE_STORED));
        doc2.add(new Field("desc", "side", TextField.TYPE_STORED));
        writer.addDocument(doc2);

    }

    private void addClients(final IndexWriter writer) throws IOException {
        final Document doc = new Document();
        final FieldType fieldType1 = new FieldType();
        fieldType1.setIndexOptions(IndexOptions.NONE);
        fieldType1.setStored(true);
        fieldType1.setTokenized(false);
        doc.add(new Field("id", "pt", fieldType1));
        doc.add(new Field("login", "p.task", TextField.TYPE_STORED));
        doc.add(new Field("fieldname", "Peter Task", TextField.TYPE_STORED));
        doc.add(new Field("desc", "client", TextField.TYPE_STORED));
        doc.add(new Field("type", Type.CLIENT.name(), TextField.TYPE_STORED));
        writer.addDocument(doc);
    }

    private void addTenor(final IndexWriter writer) throws IOException {
        final String[] tenors = new String[] { "Y1", "Y2", "Y3", "Y4", "Y5", "Y6", "Y7", "Y8" };

        for (final String tenor : tenors) {
            final Document doc = new Document();
            final FieldType fieldType1 = new FieldType();
            fieldType1.setIndexOptions(IndexOptions.NONE);
            fieldType1.setStored(true);
            fieldType1.setTokenized(false);
            doc.add(new Field("id", tenor, fieldType1));

            final FieldType fieldType2 = new FieldType();
            fieldType2.setIndexOptions(IndexOptions.DOCS);
            fieldType2.setStored(true);
            fieldType2.setTokenized(false);
            doc.add(new Field("fieldname", tenor, fieldType2));
            doc.add(new Field("desc", "tenor", TextField.TYPE_STORED));
            doc.add(new Field("type", Type.TENOR.name(), TextField.TYPE_STORED));

            writer.addDocument(doc);
        }

    }

    private void addAssetsType(final IndexWriter iwriter) throws IOException {
        final Document doc1 = new Document();
        final FieldType fieldType1 = new FieldType();
        fieldType1.setIndexOptions(IndexOptions.NONE);
        fieldType1.setStored(true);
        fieldType1.setTokenized(false);
        doc1.add(new Field("id", "-1", fieldType1));
        doc1.add(new Field("fieldname", "Asset SWAP", TextField.TYPE_STORED));
        doc1.add(new Field("fieldname", "SWAP", TextField.TYPE_STORED));
        doc1.add(new Field("desc", "asset class", TextField.TYPE_STORED));
        doc1.add(new Field("type", Type.CLASS.name(), TextField.TYPE_STORED));
        iwriter.addDocument(doc1);

        final Document doc2 = new Document();
        final FieldType fieldType2 = new FieldType();
        fieldType2.setIndexOptions(IndexOptions.NONE);
        fieldType2.setStored(true);
        fieldType2.setTokenized(false);
        doc2.add(new Field("id", "-2", fieldType2));
        doc2.add(new Field("fieldname", "Single IRS", TextField.TYPE_STORED));
        doc2.add(new Field("fieldname", "IRS", TextField.TYPE_STORED));
        doc2.add(new Field("desc", "asset class", TextField.TYPE_STORED));
        doc2.add(new Field("type", Type.CLASS.name(), TextField.TYPE_STORED));
        iwriter.addDocument(doc2);
    }

    public void text(final String text) {
        callTerminalMethod("text", text);
    }

    @Override
    public void onTerminalEvent(final PTerminalEvent event) {
        final String requestType = event.getJsonObject().getString("type");

        if ("remove".equals(requestType)) {

        } else if ("add".equals(requestType)) {
            final String tag = event.getJsonObject().getString("tag").toLowerCase();
            final String tagID = event.getJsonObject().getString("id");
            System.err.println(tag);

            if (tag.contains("@")) {
                System.err.println("Qty@Px detected");

                final String[] split = tag.split("@");

                System.err.println("qty : " + split[0]);
                System.err.println("price : " + split[1]);

                final JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("id", tagID);
                builder.add("oldTag", tag);
                builder.add("newTag", tag);
                builder.add("desc", "Qty@Px");
                builder.add("found", "yes");
                builder.add("type", "1");
                callTerminalMethod("updateTag", builder);
                return;
            }

            final Term term = new Term("fieldname", tag);
            final Query query = new FuzzyQuery(term);
            // final TopDocs hits = isearcher.search(query, 1000).scoreDocs;

            // final Query query = parser.parse("indeed");
            final ScoreDoc[] hits;
            try {
                hits = isearcher.search(query, 1000).scoreDocs;
                // Iterate through the results:
                for (final ScoreDoc hit : hits) {
                    System.err.println("Score : " + hit.score);
                    final Document hitDoc = isearcher.doc(hit.doc);
                    final String stringValue = hitDoc.getField("fieldname").stringValue();

                    final Type type = Type.valueOf(hitDoc.getField("fieldname").stringValue());

                    System.err.println("Found document" + stringValue);
                    System.err.println("Custom Data" + hitDoc.getField("id").stringValue());

                    final IndexableField description = hitDoc.getField("desc");
                    String descriptionString = "";
                    if (description != null) {
                        descriptionString = description.stringValue();
                    }

                    if (Objects.equals(stringValue, tag)) {
                        return;
                    }

                    switch (type) {
                        case SIDE:
                            if (selectedSide == null) {
                                selectedSide = stringValue;
                            } else {
                                final JsonObjectBuilder builder = Json.createObjectBuilder();
                                builder.add("id", tagID);
                                builder.add("oldTag", tag);
                                builder.add("found", "no");
                                builder.add("desc", "side is already selected");
                                callTerminalMethod("updateTag", builder);
                                return;
                            }
                            break;
                        default:
                            break;
                    }

                    final JsonObjectBuilder builder = Json.createObjectBuilder();
                    builder.add("id", tagID);
                    builder.add("found", "yes");
                    builder.add("oldTag", tag);
                    builder.add("newTag", stringValue);
                    builder.add("desc", descriptionString);
                    callTerminalMethod("updateTag", builder);
                    return;
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }

            // NO result found

            final JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("id", tagID);
            builder.add("oldTag", tag);
            builder.add("found", "no");
            callTerminalMethod("updateTag", builder);
        }

    }

    public void selectBuy(final Boolean selected) {
        if (selected) {
            if (selectedSide == null) {
                final JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("found", "yes");
                builder.add("tag", "Buy");
                builder.add("desc", "side");
                callTerminalMethod("addTag", builder);
            }
        } else {

        }
    }

    public void selectSell(final Boolean selected) {
        if (selected) {

        } else {

        }
    }

}
