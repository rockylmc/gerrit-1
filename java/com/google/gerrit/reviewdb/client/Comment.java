// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.reviewdb.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.gerrit.common.Nullable;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Objects;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;

/**
 * This class represents inline comments in NoteDb. This means it determines the JSON format for
 * inline comments in the revision notes that NoteDb uses to persist inline comments.
 *
 * <p>Changing fields in this class changes the storage format of inline comments in NoteDb and may
 * require a corresponding data migration (adding new optional fields is generally okay).
 *
 * <p>{@link PatchLineComment} historically represented comments in ReviewDb. There are a few
 * notable differences:
 *
 * <ul>
 *   <li>PatchLineComment knows the comment status (published or draft). For comments in NoteDb the
 *       status is determined by the branch in which they are stored (published comments are stored
 *       in the change meta ref; draft comments are store in refs/draft-comments branches in
 *       All-Users). Hence Comment doesn't need to contain the status, but the status is implicitly
 *       known by where the comments are read from.
 *   <li>PatchLineComment knows the change ID. For comments in NoteDb, the change ID is determined
 *       by the branch in which they are stored (the ref name contains the change ID). Hence Comment
 *       doesn't need to contain the change ID, but the change ID is implicitly known by where the
 *       comments are read from.
 * </ul>
 *
 * <p>For all utility classes and middle layer functionality using Comment over PatchLineComment is
 * preferred, as ReviewDb is gone so PatchLineComment is slated for deletion as well. This means
 * Comment should be used everywhere and only for storing inline comment in ReviewDb a conversion to
 * PatchLineComment is done. Converting Comments to PatchLineComments and vice verse is done by
 * CommentsUtil#toPatchLineComments(Change.Id, PatchLineComment.Status, Iterable) and
 * CommentsUtil#toComments(String, Iterable).
 */
public class Comment {
  public static class Key {
    public String uuid;
    public String filename;
    public int patchSetId;

    public Key(Key k) {
      this(k.uuid, k.filename, k.patchSetId);
    }

    public Key(String uuid, String filename, int patchSetId) {
      this.uuid = uuid;
      this.filename = filename;
      this.patchSetId = patchSetId;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("uuid", uuid)
          .add("filename", filename)
          .add("patchSetId", patchSetId)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Key) {
        Key k = (Key) o;
        return Objects.equals(uuid, k.uuid)
            && Objects.equals(filename, k.filename)
            && Objects.equals(patchSetId, k.patchSetId);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(uuid, filename, patchSetId);
    }
  }

  public static class Identity {
    int id;

    public Identity(Account.Id id) {
      this.id = id.get();
    }

    public Account.Id getId() {
      return Account.id(id);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Identity) {
        return Objects.equals(id, ((Identity) o).id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("id", id).toString();
    }
  }

  public static class Range implements Comparable<Range> {
    private static final Comparator<Range> RANGE_COMPARATOR =
        Comparator.<Range>comparingInt(range -> range.startLine)
            .thenComparingInt(range -> range.startChar)
            .thenComparingInt(range -> range.endLine)
            .thenComparingInt(range -> range.endChar);

    public int startLine; // 1-based, inclusive
    public int startChar; // 0-based, inclusive
    public int endLine; // 1-based, exclusive
    public int endChar; // 0-based, exclusive

    public Range(Range r) {
      this(r.startLine, r.startChar, r.endLine, r.endChar);
    }

    public Range(com.google.gerrit.extensions.client.Comment.Range r) {
      this(r.startLine, r.startCharacter, r.endLine, r.endCharacter);
    }

    public Range(int startLine, int startChar, int endLine, int endChar) {
      this.startLine = startLine;
      this.startChar = startChar;
      this.endLine = endLine;
      this.endChar = endChar;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Range) {
        Range r = (Range) o;
        return Objects.equals(startLine, r.startLine)
            && Objects.equals(startChar, r.startChar)
            && Objects.equals(endLine, r.endLine)
            && Objects.equals(endChar, r.endChar);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(startLine, startChar, endLine, endChar);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("startLine", startLine)
          .add("startChar", startChar)
          .add("endLine", endLine)
          .add("endChar", endChar)
          .toString();
    }

    @Override
    public int compareTo(Range otherRange) {
      return RANGE_COMPARATOR.compare(this, otherRange);
    }
  }

  public Key key;
  /** The line number (1-based) to which the comment refers, or 0 for a file comment. */
  public int lineNbr;

  public Identity author;
  protected Identity realAuthor;
  public Timestamp writtenOn;
  public short side;
  public String message;
  public String parentUuid;
  public Range range;
  public String tag;

  // Hex commit SHA1 of the commit of the patchset to which this comment applies. Other classes call
  // this "commitId", but this class uses the old ReviewDb term "revId", and this field name is
  // serialized into JSON in NoteDb, so it can't easily be changed. Callers do not access this field
  // directly, and instead use the public getter/setter that wraps an ObjectId.
  private String revId;

  public String serverId;
  public boolean unresolved;

  /**
   * Whether the comment was parsed from a JSON representation (false) or the legacy custom notes
   * format (true).
   */
  public transient boolean legacyFormat;

  public Comment(Comment c) {
    this(
        new Key(c.key),
        c.author.getId(),
        new Timestamp(c.writtenOn.getTime()),
        c.side,
        c.message,
        c.serverId,
        c.unresolved);
    this.lineNbr = c.lineNbr;
    this.realAuthor = c.realAuthor;
    this.range = c.range != null ? new Range(c.range) : null;
    this.tag = c.tag;
    this.revId = c.revId;
    this.unresolved = c.unresolved;
  }

  public Comment(
      Key key,
      Account.Id author,
      Timestamp writtenOn,
      short side,
      String message,
      String serverId,
      boolean unresolved) {
    this.key = key;
    this.author = new Comment.Identity(author);
    this.realAuthor = this.author;
    this.writtenOn = writtenOn;
    this.side = side;
    this.message = message;
    this.serverId = serverId;
    this.unresolved = unresolved;
  }

  public void setLineNbrAndRange(
      Integer lineNbr, com.google.gerrit.extensions.client.Comment.Range range) {
    this.lineNbr = lineNbr != null ? lineNbr : range != null ? range.endLine : 0;
    if (range != null) {
      this.range = new Comment.Range(range);
    }
  }

  public void setRange(CommentRange range) {
    this.range = range != null ? range.asCommentRange() : null;
  }

  @Nullable
  public ObjectId getCommitId() {
    return revId != null ? ObjectId.fromString(revId) : null;
  }

  public void setCommitId(@Nullable AnyObjectId commitId) {
    this.revId = commitId != null ? commitId.name() : null;
  }

  public void setRealAuthor(Account.Id id) {
    realAuthor = id != null && id.get() != author.id ? new Comment.Identity(id) : null;
  }

  public Identity getRealAuthor() {
    return realAuthor != null ? realAuthor : author;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Comment)) {
      return false;
    }
    Comment c = (Comment) o;
    return Objects.equals(key, c.key)
        && lineNbr == c.lineNbr
        && Objects.equals(author, c.author)
        && Objects.equals(realAuthor, c.realAuthor)
        && Objects.equals(writtenOn, c.writtenOn)
        && side == c.side
        && Objects.equals(message, c.message)
        && Objects.equals(parentUuid, c.parentUuid)
        && Objects.equals(range, c.range)
        && Objects.equals(tag, c.tag)
        && Objects.equals(revId, c.revId)
        && Objects.equals(serverId, c.serverId)
        && unresolved == c.unresolved;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        key,
        lineNbr,
        author,
        realAuthor,
        writtenOn,
        side,
        message,
        parentUuid,
        range,
        tag,
        revId,
        serverId,
        unresolved);
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

  protected ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this)
        .add("key", key)
        .add("lineNbr", lineNbr)
        .add("author", author.getId())
        .add("realAuthor", realAuthor != null ? realAuthor.getId() : "")
        .add("writtenOn", writtenOn)
        .add("side", side)
        .add("message", Objects.toString(message, ""))
        .add("parentUuid", Objects.toString(parentUuid, ""))
        .add("range", Objects.toString(range, ""))
        .add("revId", Objects.toString(revId, ""))
        .add("tag", Objects.toString(tag, ""))
        .add("unresolved", unresolved);
  }
}
