package com.epimorphics.lda.renderers.velocity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epimorphics.vocabs.API;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Help {

	static String labelFor( Resource r ) {
		Statement s = r.getProperty( RDFS.label );
		return s == null ? r.getLocalName() : s.getString();
	}

	public static Map<String, Object> getMetadataFrom( Map<Resource, String> shortNames, Model m ) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Resource> pages = m.listSubjectsWithProperty( RDF.type, API.Page ).toList();
		for (Resource p: pages) {
			List<Statement> wrs = p.listProperties( API.wasResultOf ).toList();
			for (Statement wr: wrs) {
				descend( shortNames, result, "", wr.getResource() );
			}
		}
		return result;
	}

	private static void descend( Map<Resource, String> shortNames, Map<String, Object> result, String prefix, RDFNode r) {
		if (r.isResource() && hasProperties( r.asResource() )) {
			Resource rr = r.asResource();
			for (Statement s: rr.listProperties().toList()) {
				Resource p = s.getPredicate();
				String pn = shortNames.get(p);
				if (pn == null) pn = p.getLocalName();
				descend( shortNames, result, (prefix.isEmpty() ? pn : prefix + "." + pn), s.getObject() );
			}
		} else 
			result.put( prefix, new Item( r ) );
	}

	static final Property ANY = null;
	
	private static boolean hasProperties(Resource r) {
		return r.getModel().listStatements( r, ANY, ANY ).hasNext();
	}

	public static Map<Resource, String> getShortnames( Model m ) {
		Map<Resource, String> uriToShortname = new HashMap<Resource, String>();
		List<Resource> pages = m.listSubjectsWithProperty( RDF.type, API.Page ).toList();
		if (pages.size() == 1) {
			List<Statement> results = pages.get(0).listProperties( API.wasResultOf ).toList();
			if (results.size() == 1) {
				List<Statement> bindings = results.get(0).getResource().listProperties( API.termBinding ).toList();
				for (Statement sb: bindings) {
					String sn = sb.getProperty( API.label ).getString();
					Resource fn = sb.getProperty( API.property ).getResource();
					uriToShortname.put( fn, sn );
				}
			}
		}
		return uriToShortname;
	}

	public static class Format {
		final String linkUsing;
		final String name;
		final String mediaType;
		
		public Format( String linkUsing, String name, String mediaType ) {
			this.linkUsing = linkUsing;
			this.name = name;
			this.mediaType = mediaType;
		}
		
		public String getName() {
			return name;
		}
		
		public String getLink() {
			return linkUsing;
		}
	}
	
	public static Set<Format> getFormats( Model m ) {
		HashSet<Format> result = new HashSet<Format>();
		List<Resource> links = m.listSubjectsWithProperty( DCTerms.format ).toList();
		for (Resource link: links) {
			String name = labelFor( link );
			String type = labelFor( link.getProperty(DCTerms.format).getResource() );
			result.add( new Format( link.getURI(), name, type ) );
		}
		return result;
	}
}