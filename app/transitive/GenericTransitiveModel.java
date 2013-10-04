package transitive;

import play.db.Model;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

@MappedSuperclass
public abstract class GenericTransitiveModel implements Model, Serializable {

    public void persist() {
        JPA.em().persist(this);
    }


    public static String join(Object ... components) {
        StringBuilder sb = new StringBuilder();
        for(Object o : components) {
            sb.append(o!=null ? o.toString() : null);
        }
        return sb.toString();
    }

    public static String qq(String s) {
        if(s==null) return null;
        StringBuilder sb = new StringBuilder("\"");
        sb.append(s);
        sb.append("\"");
        return sb.toString();
    }

    /**
     * Count entities
     * @return number of entities of this class
     */
    public static long count() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Count entities with a special query.
     * Example : Long moderatedPosts = Post.count("moderated", true);
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A long
     */
    public static long count(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find all entities of this type
     */
    public static <T extends GenericTransitiveModel> List<T> findAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find the entity with the corresponding id.
     * @param id The entity id
     * @return The entity
     */
    public static <T extends GenericTransitiveModel> T findById(Object id) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Prepare a query to find entities.
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A JPAQuery
     */
    public static JPAQuery find(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Prepare a query to find *all* entities.
     * @return A JPAQuery
     */
    public static JPAQuery all() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Batch delete of entities
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return Number of entities deleted
     */
    public static int delete(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Delete all entities
     * @return Number of entities deleted
     */
    public static int deleteAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }


    public static class JPAQueryException extends RuntimeException {

        public JPAQueryException(String message) {
            super(message);
        }

        public JPAQueryException(String message, Throwable e) {
            super(message + ": " + e.getMessage(), e);
        }

        public static Throwable findBestCause(Throwable e) {
            Throwable best = e;
            Throwable cause = e;
            int it = 0;
            while ((cause = cause.getCause()) != null && it++ < 10) {
                if (cause instanceof ClassCastException) {
                    best = cause;
                    break;
                }
                if (cause instanceof SQLException) {
                    best = cause;
                    break;
                }
            }
            return best;
        }
    }


    /**
     * A JPAQuery
     */
    public static class JPAQuery {

        public Query query;
        public String sq;

        public JPAQuery(String sq, Query query) {
            this.query = query;
            this.sq = sq;
        }

        public JPAQuery(Query query) {
            this.query = query;
            this.sq = query.toString();
        }

        public <T> T first() {
            try {
                List<T> results = query.setMaxResults(1).getResultList();
                if (results.isEmpty()) {
                    return null;
                }
                return results.get(0);
            } catch (Exception e) {
                throw new GenericTransitiveModel.JPAQueryException("Error while executing query <strong>" + sq + "</strong>", GenericTransitiveModel.JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Bind a JPQL named parameter to the current query.
         */
        public JPAQuery bind(String name, Object param) {
            if (param.getClass().isArray()) {
                param = Arrays.asList((Object[]) param);
            }
            if (param instanceof Integer) {
                param = ((Integer) param).longValue();
            }
            query.setParameter(name, param);
            return this;
        }

        /**
         * Retrieve all results of the query
         * @return A list of entities
         */
        public <T> List<T> fetch() {
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new GenericTransitiveModel.JPAQueryException("Error while executing query <strong>" + sq + "</strong>", GenericTransitiveModel.JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Retrieve results of the query
         * @param max Max results to fetch
         * @return A list of entities
         */
        public <T> List<T> fetch(int max) {
            try {
                query.setMaxResults(max);
                return query.getResultList();
            } catch (Exception e) {
                throw new GenericTransitiveModel.JPAQueryException("Error while executing query <strong>" + sq + "</strong>", GenericTransitiveModel.JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Set the position to start
         * @param position Position of the first element
         * @return A new query
         */
        public <T> JPAQuery from(int position) {
            query.setFirstResult(position);
            return this;
        }

        /**
         * Retrieve a page of result
         * @param page Page number (start at 1)
         * @param length (page length)
         * @return a list of entities
         */
        public <T> List<T> fetch(int page, int length) {
            if (page < 1) {
                page = 1;
            }
            query.setFirstResult((page - 1) * length);
            query.setMaxResults(length);
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new GenericTransitiveModel.JPAQueryException("Error while executing query <strong>" + sq + "</strong>", GenericTransitiveModel.JPAQueryException.findBestCause(e));
            }
        }
    }
}

