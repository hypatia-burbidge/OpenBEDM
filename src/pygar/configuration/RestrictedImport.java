	/****************************************************************CopyrightNotice
	 * Copyright (c) 2011 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *******************************************************************************/

package pygar.configuration;

/** The use of modules annotated with @RestrictedImport should be limited 
 * because these modules have an ability to access security
 * sensitive files and databases. They should only be imported into tested and 
 * approved applications. Note, this documentation also contains a general 
 * discussion of the security philosophy behind the security zones and
 * the various annotations such as @RestrictedImport. Please see below.
 * 
 * <p><em>Summary of Restrictions</em></p>
 * <ul>
 * <li>The private key of the local entity should only be used to sign
 * outgoing documents and to decrypt documents sent to this entity.
 * <li>The public key of other entities should only be used to encrypt
 * documents sent to that outside entity and conversely to verify the
 * signature of documents sent by the outside entity.
 * </ul> 
 * 
 * <p><em>Prevention of Malware Intrusion with Careful Software Design</em></p>
 * <p>There is absolutely no way to prevent the intrusion of malware - viruses, worms, 
 * spyware, etc. - with software alone. Physical security is of paramount importance 
 * followed by network firewall defenses. In addition, security managers must contend with
 * the danger that an insider tries to
 * install unauthorized software to compromise security. Proper software design can
 * make it easier to defend against such an insider threat. 
 * </p><p>
 * Our philosophy is that the core software should be simple, clear, and readily verifiable
 * by inspection. Next we combine that decision with annotations that flag modules that should
 * only be used with verified modules. For example, modules that access keystores should be run
 * only by verified, core software. Just by preventing access to keystores, we can prevent the
 * forgery of documents, for example. The integrity of this system can be checked by verifying 
 * that the sensitive modules are imported only by modules that were verified and approved
 * to use the sensitive module.
 * </p><p>
 * In addition, the software is divided into zones. There is no automatic way to use the zones
 * to any advantage. However, any organization that is very conscious of security can arrange 
 * an architecture of computers that allocates the software zones on different processors and 
 * separates them with internal firewalls. When that approach is taken, legitimate work of the
 * system is performed efficiently without impediment but malware that finds it way into one
 * zone or another is unable to obtain information or communicate with its outside controllers. 
 * </p>
 * <p><em>Status of the Implementation of Zones</em></p>
 * <p>Although specific zones were implemented as named packages in early versions of the software, various
 * practical and theoretical problems lead to the abandonment of the first design. The most
 * obvious problem is that some modules are needed in two zones - so how do we assign them to
 * two packages? The solution will involve separating the packages into zoned software when the 
 * releases for each zone are built. This new solution has not yet been implemented although
 * we have identified the modules that are involved in zoning by assigning them to the 
 * package "zoneable". In the absence of a finished zone system in this release, we are
 * keeping the documentation for the first design of zones as an example of one type of
 * zone architecture.
 * </p>
 * <p><em>The Old Zone System Design - for illustration only</em></p>
 * <ul>
 * <li><b>Zone 10</b>: this is the innermost, most protected zone. A client of the system
 * keeps the secret data in Zone 10 and composes the documents here. Any new document is 
 * partially encrypted with the session key before leaving the zone. On the other hand, 
 * software in this zone cannot communicate with the world directly and it cannot use keys
 * or certificates to encrypt or sign messages. In the match-maker, Zone 10 contains the 
 * actual match-maker process. The match-maker has no session keys so the data entering
 * this zone is partially encrypted and remains so. Data leaving is formed from encrypted
 * data and is thus also encrypted.
 * <li><b>Zone 6</b>: In Zone 6 incoming data is decrypted using the private key of the 
 * current system entity. Thus, any data that was not intended for this entity is rejected.
 * More importantly, very few components have access to to private key, thus, it is difficult
 * for malware to read data documents intended for this entity. Outgoing data in Zone 6 is
 * encrypted with the public key of the intended destination to ensure that only the 
 * destination entity reads the data. Note that this encryption even protects against 
 * malware operating in Zones 3 and 0.
 * <li><b>Zone 3</b>:In Zone 3, the signature of incoming data is verified with the public key
 * of the source entity. If the signature is not verified, the data is rejected. Also in this
 * zone, this entity signs all outgoing documents using its private key. 
 * <li><b>Zone 0</b>:In Zone 0, both incoming and outgoing documents have three layers of encryption
 * established by the inner zones. Thus, this zone has the lowest security requirement. However,
 * the zone must still be protected as well as possible from denial of service attacks and other
 * outside attempts to damage the system.
 * </ul>
 * <p>The system has three types of keys: session keys, public keys associated with entities, and the 
 * public/private key-pair of the local entity used by the public-key-encryption system . 
 * If each keytype in its own keystore
 * then we can restrict access to just those modules that have a legitimate reasons to use a key.
 * For this reason there are three keystores: <ul>
 * <li><b>public_keystore</b>: the storage for public keys for other entities and the public 
 * certificate for this entity. This is used only in Zones 3 and 6. 
 * <li><b>private_keystore</b>: the storage for the private key of this entity. This is used only in Zones 3 and 6.
 * <li><b>session_keystore</b>: the storage for sessions. Note, this is used only in Zone 10
 * with a client and never in the match-maker. 
 *  </ul>
 *  Note that we can restrict the session_keystore to Zone 10, but the other two stores are needed in 
 *  both Zone 3 and 6 because of the different inbound and outbound processing. Nevertheless, no legitimate
 *  operation in this system requires more than one keystore to accomplish its function. 
 *  Any module that imports two
 *  keystores is suspect and should not be trusted.
 * 
 * @author pbaker
 *
 */
public @interface RestrictedImport {

}
