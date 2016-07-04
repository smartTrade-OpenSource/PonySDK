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

import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;

public class SelectizeAddon extends PAddOnComposite<PElement> implements PTerminalEvent.Handler {

    IndexSearcher isearcher = null;

    public SelectizeAddon() {
        super(new PElement("input"));
        asWidget().setAttribute("value", "science,biology,chemistry,physics");
        setTerminalHandler(this);

        //
        final Analyzer analyzer = new StandardAnalyzer();
        final Directory directory = new RAMDirectory();

        final IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter;
        try {
            iwriter = new IndexWriter(directory, config);
            final Document doc = new Document();
            final String text = "Je taime ma ninette.";
            doc.add(new Field("fieldname", text, TextField.TYPE_STORED));

            iwriter.addDocument(doc);
            iwriter.close();
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
            //ireader.close();
            //directory.close();
        } catch (final Exception exception) {
            exception.printStackTrace();
        }


        // <input type="text" id="input-tags3" class="demo-default"
        // value="science,biology,chemistry,physics">
    }

    public void text(final String text) {
        callTerminalMethod("text", text);
    }

    @Override
    public void onTerminalEvent(final PTerminalEvent event) {
        final String tag = event.getJsonObject().getString("tag");
        System.err.println(tag);

        String tagID = event.getJsonObject().getString("id");
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
                System.err.println("Found document" + hitDoc.getField("fieldname").stringValue());

                final JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("id",tagID);
                builder.add("oldTag",tag);
                builder.add("newTag",hitDoc.getField("fieldname").stringValue());

                callTerminalMethod("tag", builder);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
