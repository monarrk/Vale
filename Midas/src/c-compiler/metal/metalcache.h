#ifndef METAL_CACHE_H_
#define METAL_CACHE_H_

#include <unordered_map>

#include "metal/types.h"
#include "metal/ast.h"
#include "instructions.h"

template<typename K, typename V, typename H, typename E, typename F>
V makeIfNotPresent(std::unordered_map<K, V, H, E>* map, const K& key, F&& makeElement) {
  auto iter = map->find(key);
  if (iter == map->end()) {
    auto p = map->emplace(key, makeElement());
    iter = p.first;
  }
  return iter->second;
}

struct HashRefVec {
  size_t operator()(const std::vector<Reference *> &refs) const {
    size_t result = 1337;
    for (auto el : refs) {
      result += (size_t) el;
    }
    return result;
  }
};
struct RefVecEquals {
  bool operator()(
      const std::vector<Reference *> &a,
      const std::vector<Reference *> &b) const {
    if (a.size() != b.size())
      return false;
    for (size_t i = 0; i < a.size(); i++) {
      if (a[i] != b[i])
        return false;
    }
    return true;
  }
};

class MetalCache {
public:
  Int* innt = new Int();
  Bool* boool = new Bool();
  Str* str = new Str();
  Void* vooid = new Void();
  Never* never = new Never();

  std::unordered_map<Name*, StructReferend*> structReferends;
  std::unordered_map<Name*, InterfaceReferend*> interfaceReferends;
  std::unordered_map<std::string, Name*> names;

  // This is conceptually a map<[Reference*, Mutability], RawArrayT*>.
  std::unordered_map<Reference*, std::unordered_map<Mutability, RawArrayT*>> rawArrays;
  std::unordered_map<Name*, UnknownSizeArrayT*> unknownSizeArrays;
  std::unordered_map<Name*, KnownSizeArrayT*> knownSizeArrays;
  std::unordered_map<Referend*, std::unordered_map<Ownership, std::unordered_map<Location, Reference*>>> references;
  std::unordered_map<Name*, std::unordered_map<Reference*, std::unordered_map<std::vector<Reference*>, Prototype*, HashRefVec, RefVecEquals>>> prototypes;
  std::unordered_map<int, VariableId*> variableIds;
  std::unordered_map<VariableId*, std::unordered_map<Reference*, Local*>> locals;

  Reference* getReference(Ownership ownership, Location location, Referend* referend) {
    return makeIfNotPresent(
        &references[referend][ownership],
        location,
        [&](){ return new Reference(ownership, location, referend); });
  }
};

#endif
